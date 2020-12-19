package org.k.porn;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Main {

    public static Connection con = null;
    public static SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
    private static boolean doSaveStory = true;
    private static boolean doSaveAuthor = false;

    public static void getConnection() {
        log.info("Get BDD connection");
        String url = "jdbc:postgresql://localhost/literotica";
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "admin");
        //props.setProperty("ssl","true");
        try {
            Connection connection = DriverManager.getConnection(url, props);
            con = connection;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT VERSION()");
            if (rs.next()) {
                System.out.println(rs.getString(1));
            }

        } catch (SQLException ex) {

            Logger lgr = Logger.getLogger(Main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


    public static boolean isAuthorInBase(String key) {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT count(*) from author where id = ?");
            ps.setInt(1, Integer.valueOf(key));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return false;
    }

    public static Set<String> getAllAuthor() {
        Set<String> toReturn = new HashSet<>();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id from author");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                toReturn.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public static boolean isStoryInBase(String key) {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT count(*) from public.story where key = LOWER(?)");
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return false;
    }

    private static boolean isSerieLinkInBase(String idAuthor, String serieTitle, String key) {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT count(*) from series where id_author = ? and title= ? and chapter = ? ");
            ps.setInt(1, Integer.valueOf(idAuthor));
            ps.setString(2, serieTitle);
            ps.setString(3, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("series not insert cause by {}", e.getMessage());

        }
        return false;
    }

    public static void insertStory(String key, String title, String author, String content) {
        log.info("Insert the story {} for the author {}", title, author);
        try {
            PreparedStatement st = con.prepareStatement("INSERT INTO public.story(key,title, content, author) VALUES (lower(?),?, ?, ?)");
            st.setString(1, key);
            st.setString(2, title);
            st.setString(3, content);
            st.setString(4, author);
            st.execute();
        } catch (SQLException e) {
            log.error("story not insert cause by {}", e.getMessage());
        }
    }

    private static void insertAuthor(String idAuthor, String author) {
        log.info("Insert the author {} {}", idAuthor, author);
        try {
            PreparedStatement st = con.prepareStatement("INSERT INTO author(id, name) VALUES (?,?)");
            st.setInt(1, Integer.valueOf(idAuthor));
            st.setString(2, author);
            st.execute();
        } catch (SQLException e) {
            log.error("author not insert cause by {}", e.getMessage());
        }
    }

    public static void insertTagStory(String key, List<String> tags) {
        log.info("Insert tag {} for the story {}", tags, key);
        for (String tag : tags) {
            try {
                PreparedStatement st = con.prepareStatement("INSERT INTO public.story_tag(key, tag) VALUES (?, ?)");
                st.setString(1, key);
                st.setString(2, tag);
                st.execute();
            } catch (SQLException e) {
                log.error("tag not insert cause by {}", e.getMessage());
            }
        }
    }

    private static void insertSerieLink(String idAuthor, String title, String key, int order) {
        log.info("Insert chapter {} of series {} for the author {} ", key, title, idAuthor);
        try {
            PreparedStatement st = con.prepareStatement("INSERT INTO series(id_author, title, chapter, order_chap) VALUES (?, ?, ?, ?)");
            st.setInt(1, Integer.valueOf(idAuthor));
            st.setString(2, title);
            st.setString(3, key);
            st.setInt(4, order);
            st.execute();
        } catch (SQLException e) {
            log.error("chapter not insert cause by {}", e.getMessage());
        }
    }


    private static void addInformationStory(String key, String description, String type, Date date, String note) {
//        log.info("Add information for {} : description = {}, type= {}, date= {}, note = {}", key, description, type, date, note);
        try {
            PreparedStatement st = con.prepareStatement("update story set type= ?, description = ?, date= ?, note = ? where key = ?");
            st.setString(1, type);
            st.setString(2, description);
            st.setDate(3, convertUtilToSql(date));
            st.setString(4, note);
            st.setString(5, key);
            st.execute();
        } catch (SQLException e) {
            log.error("Information not inserted cause by {}", e.getMessage());
        }
    }

    private static java.sql.Date convertUtilToSql(java.util.Date uDate) {
        if (uDate == null) {
            return null;
        }
        java.sql.Date sDate = new java.sql.Date(uDate.getTime());
        return sDate;
    }

    public static void crollType(String type) {
        IntStream stream = IntStream.range(1, 75);

        stream.parallel().forEach(value
                        -> {
                    getAuthorLink("https://www.literotica.com/c/" + type + "/" + value + "-page");
                }
        );
    }

    public static void getAuthorLink(String urlType) {
        log.info("Crolling page {} for author", urlType);
        try {
            HttpClient client = HttpClientBuilder.create().build();
            String contentHTML = getContentHTMLFRomURL(client, urlType);
            if (contentHTML.contains("This member does not exists")) {
                return;
            }
            Document html = Jsoup.parse(contentHTML);
            Elements linkAuthor = html.select(".b-sli-author a");
            Set<String> listUrl = linkAuthor.stream().map(td -> getHref(td)).collect(Collectors.toSet());
            listUrl.parallelStream().forEach(lien -> {
                saveStoryOfAuthor(client, lien);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void searchUsers() {
        List<String> listName = Arrays.asList("ringdingaling48", "andyxx338", "LostGynoid", "look_no_panties", "jingo", "allhornysomecorny", "sassylassy82", "mickeystacks", "GingerPAWG", "Heywood Jablomy", "RoyallyPink", "ChronicMB", "glam_rocker", "spunkynq", "drokk16", "southernman_ga1", "KachineKoko", "drippingwetsub", "lustylovedelilah", "Wisconsin Bookie", "boyswillbeboys", "Selafyn", "Fair_Deuce", "DanielBrown", "fattulip", "kelseykink", "Vera_Collette", "mftalbot", "appleaday", "kimkitten26", "IsoldeLovesTristan", "Bulldog63", "elelar", "theodorelocke", "Mirrored", "misanthrope75", "LadyDragoness623", "CeirdwynMaraBones", "PorscheLynn", "anthonyparallel", "dissolvedgirl", "PrudenceSunshine", "babydoll99", "Allyrianna", "corpusconfero", "BigMouse", "ana_fall", "wantedone", "coronova", "Eurika", "PhoebeMagdalena", "Dani2001", "LilSliceOfCherryPie", "Hektorus", "catchherorgasm", "recordshop", "welshpaddy", "advantagetaken", "TSA1984", "SeattleRain", "madamnight", "Tonights_Amadeus", "Cashmere Kyss", "CleavageToBeaver", "armywife82", "Lucky3", "kellianne", "TheGWAG", "YogaDude30", "FairyEyez", "V1xen", "partyflavors", "Drpepwer", "McSarah_Sora", "daniedevoe", "PrettyPrettyPrincess", "felt", "secretwishes", "JCom", "tiffday", "xblondie2235x", "Shiranui1", "CrazyOldLady", "ebethalize", "Torilikesitcrazy", "pokerdegen", "whitebbygrl4523", "amazingalicia", "Sexy_Poet", "Bringmesomewater", "JesseJohnson", "JessiePi", "All_atremble", "QueenB77", "Time2GL", "sobold", "EnglishTongue", "isxa", "supersucker", "Helly_Robin", "joysstory", "Surreallemming", "kozak", "an0nym0uss", "Desmondo66", "4phr0d1te", "Tess73", "eve362", "tufguy350", "doubleboast", "SophButtons", "ImaginaryLover70", "WoodyTheWise", "Anklegoddess2", "Valet of Roissy", "Juspar Emvan", "redneckbillionaire", "MrsDilby0611", "hasabrain2", "kinkyvixen", "BubbleBerry", "Steffee", "allie_cromley", "oralperfection2", "tripstarryeyed", "mrsoggypecs2005", "Synjyn", "goodrobert8", "mpknight", "TopInDenial", "jackh5577", "joshnmeg", "LingerieLouise", "Bookbinder", "TheMalevolence", "jessicaga89", "electrictingles", "cmw90", "eagleyez", "john_johnson", "Guido83", "tcrass", "charlicox", "DelphiniumBlue", "PussyPirate2010", "sindypink", "ladylove", "Downundergirl79", "SweetHat", "leftie", "Caferacer1960", "Peara", "lilvixenchica", "alwaysdying", "designergirl", "kandymountain", "KinkyCouple69", "bighands99", "luxxxury", "Jman2150", "GDeloney", "technolover", "brookes", "letsgofucksome_honeybuns", "mysty_one", "realclear", "Sweet_Pea24", "JoeBeans275", "seductionsong", "puits_d_amour", "cricketbug", "sexity", "Functional_Perv", "baby_face_b", "Amber jayne", "HotPeter", "lana97", "natureangel", "Barbaraallsybian", "incotula", "r_o_w_d_y", "Bill W", "Convict26", "MisssSelene", "The_crescentmoon", "galileo181", "miss_aubergine", "Squawk00", "agnetha100", "NoOtherName", "theaverageindianmale", "bhoff84", "Juliamoretti", "LydiaLorne", "quietone", "DrFreud", "MindPortal", "stretchncatch", "ltlfoxyridr", "radiatingeyes", "XxSexYItaliaNxX", "Floyd_Bariscale", "JLPE15", "smutsherface", "Imasturbatedaily", "HeidiBlossom", "just_wondering_man", "MishaMoon", "missyinmt", "naughtyblondie", "straighttoapoint", "Aminathius", "storyteller_anon", "lolly666", "vinoveritas1982", "onandonand", "mynaughtypix", "candysweet81", "havingfun01", "erotiqg", "VrgnKat", "HollyMunroex", "SerenaLeigh", "Champagne_Supernova", "Silverlily", "livinnsinptwn", "TerpsichoreinThalia", "HornyDreamer16", "majal", "endofauniverse", "Sultry Tease", "jo503", "wishinandhopin", "Bridesmaid", "CJ", "russiandoll", "princessbedhead", "Amandas_secret", "JMR85", "ShamelessBratt", "bie", "mrsdozer", "PinkAndWhite", "Scornelius", "trit 7", "AudreyBloome", "Jimmorison47", "Go_east", "Thinking_Outloud", "Intermezzo_Erotique", "Jakeza", "cumr", "Last17Steps", "NYNiteRiter", "warmthinthedark", "TigerTigerBurningBright", "NothernBelle", "MaxPower22", "LucyOctober", "browneyed_demonic_angel", "timyellow", "Nuitloupe", "BBWSaphrina", "VioletKisses", "Aleirian", "moondol", "LanceSapikowski", "Roxrho", "xmarksthespot", "2balternative", "ambitious07", "Techomancer", "shavemeorcry", "KarlFive", "auslit", "Boofyboyd", "intellectuallyfrustrated", "GemAri", "ARobinsHood", "Coalporter", "SsWeEnEsTation", "thecorruptedteacups", "MidnighTWhisperS", "asktoday32", "CandyBJ", "raveninflight", "candlesfyre", "Jani715", "dmh1dgls", "cwdag", "JessieLyons", "tulips6684", "DiyanaGalanis", "noobwriter", "lustful_wonder", "AJamesDarkly", "veiled", "maruian", "KinghtlySense", "Shezalady", "Afterschoolspecial", "Jayce_A", "kyliehot2007", "sexycareerwoman", "gamblin_man", "coloquintida", "dirtylittledrawings", "HirstWood", "insects_of_caring", "asphyxiation", "poebassmn", "JuliaEV", "Safefunguy", "HiddenVoyeur88", "greatbondage", "kaleidoscopeyes", "kelseakerfuffle", "RobinPeacock", "guitargal81", "TPPD", "Scarlett1993", "Long_way_off", "MyRealAutobiographyOfSex", "ozgood948", "JAGie", "The_Diviner", "HeloJeff", "DeepTouch", "clairelenore", "ChucksThoughts", "pplayer", "Jay_nice", "sry2bthryou", "lickerish", "SlaveInTraining", "fullpassionking", "kallindrathehorny", "Herzog11", "Athene", "megdarlin", "letia", "bee1000", "b7brad63", "gp2202", "andrewgoogle", "Distantfriend", "saabigtime", "mraynald", "CoffeeGal", "pjhughes25", "RichFantasylife", "badcharlotte", "AzureZeura", "Natstar", "BareBackRider", "hobby144", "Bob Briton", "PrincessHoney", "whiskylove", "SweetSassyVixen", "Dan1382", "xTripleXQueenx", "ElfElrond", "lightscadence", "b_lover", "PinkPinstripes", "jcstroker", "Leatherargento", "Daish", "NottinghamHarry", "sxy20smthg", "sloeburn", "manmeltr", "SonicErotica", "Tagmus2", "boredblonde26", "cum_on_man", "Greynyc", "mcarden75", "ripeandproper", "mau_mau69", "AllAboutlegs", "notyouraveragewriter", "pinball_fun", "justme7", "Lust Engine", "Xtacyincin", "Lucy A", "Fallen_76", "Dv8d_121", "Lonely Empath", "Ronald10021", "OutofKYGuy", "Tom D", "gmpok", "littlefatfuckwit", "00_00", "Brave Maximus", "Bingo_Little", "AnalAnnie99", "JumpinJD", "Horny_Techie", "biboybabe", "princessdany", "cheerleader_of_doom", "bi_brandytx", "rubberjen", "sweeterred", "maxlongun", "NiZi", "writingmakesmehot", "anal_invasion2", "LadyRavn", "Apollo Wilde", "henrywoodenindian", "flirtalicious", "amend", "aquarius0131", "AvySweet", "bitty_wanter", "sng85", "Plasmawarrior", "sexually__confused", "martin_shecter1971", "kingerob", "steve_au", "luvspell", "jones bros", "pvclook", "Apophis99", "FoxTied", "theWriterInTheNude", "afficionada", "ClaraMills", "inneedofrelease", "Buggwomann", "Thalassatx", "NickPaga", "BulgyLover", "geronimox16", "Vixen54", "naughtylilschoolgirl", "CyTheWriter", "Abercr0mbieBoi", "welshandproud", "Mila_Roe", "NatashaKerenin", "gowiescorp", "NightWriter91", "alyssaL1234", "horny2233", "Babygirlmindy", "ChastitySommers", "coyotegawd", "lovetomakehercome", "HikingThrough", "basool", "Gapchenko", "shescurious", "SweetLissa", "hornystonerr420", "VGodfrey", "Waiting4mylover", "Kittycat13", "WhipKitty", "Silversiren", "Drenchxoxo", "want2watch2004", "TheOfficeBitch", "WithMyHands", "daves40004", "ThatWhichIsPure", "roxymoves", "SL_Beauty", "breastfan", "gotmilf", "leahlove", "missbhaven1968", "GOODFORYOU2", "USMC75", "BrutusBuckeye", "fsas20", "Tythos", "Reflection83", "Ravens_Dove", "SubNicki16", "Krystal_Diva", "Frankiezz3", "nadeemalim", "AbigailAnne", "SassyNNasty", "TremblingTexan01", "Tailgunner69", "theinferno", "Duaflex", "warmme3", "AwokenDesire", "Libertas_nyx", "charliechoc", "dpepp27", "The_Great_Cerdan", "ZomZolom", "citizen1828", "Rbk_47", "arisesirdicky", "Bosq", "MajorMadness", "DZYankee", "missrandom", "petezoom", "Shadowtech123", "Decovampire", "OldHippieInOz", "SiobhanSaor", "Snowz", "lace mayhem", "night_flyer_3", "mrhoppity", "kdini", "FortunesFool", "minxyfoxy", "Adrian10", "robyn_blais", "hotchickk17", "Caramel Wish", "VBallNatalie182", "zJuliaInTX", "Pet_my_Kitten", "caseyfurlow", "D_Burrows", "Greg82", "SassyBird", "shackieman", "rolechick22", "schmant", "Jinx_me", "Entricity", "BlueLeo", "Viper92", "freecici_xo", "Elainexxx24", "racer252000", "SirenedBlueStar", "peachesncreamy", "pazlyra", "daddy_o51", "Darkw0rds", "Bibrarian", "vegina", "lightnin", "toymad1", "Deepbluedream090", "AndroneseBis");
       /*listName.parallelStream().forEach(value -> {
                String urlSearch = "https://search.literotica.com/?type=member&query="+value.replace(" ", "%20");
                    HttpClient client = HttpClientBuilder.create().build();
                    try {
                        String contentHTML = getContentHTMLFRomURL(client, urlSearch);
                        Document html = Jsoup.parse(contentHTML);
                        String link = getHref(html.select(".u_eC").get(0));
                        saveStoryOfAuthor(client, link);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        */
        saveStoryOfAuthor(HttpClientBuilder.create().build(), "https://www.literotica.com/stories/memberpage.php?uid=4791342&page=submissions");
        saveStoryOfAuthor(HttpClientBuilder.create().build(), "https://www.literotica.com/stories/memberpage.php?uid=5344003&page=submissions");
        saveStoryOfAuthor(HttpClientBuilder.create().build(), "https://www.literotica.com/stories/memberpage.php?uid=2701&page=submissions");
        saveStoryOfAuthor(HttpClientBuilder.create().build(), "https://www.literotica.com/stories/memberpage.php?uid=237778&page=submissions");
        saveStoryOfAuthor(HttpClientBuilder.create().build(), "https://www.literotica.com/stories/memberpage.php?uid=4291&page=submissions");

    }

    public static void crollAllType() {
        String[] param = {"illustrated-erotic-fiction", "anal-sex-stories", "bdsm-stories", "celebrity-stories", "chain-stories", "erotic-couplings"
        ,"erotic-horror", "exhibitionist-voyeur", "fetish-stories", "first-time-sex-stories", "group-sex-stories", "gay-sex-stories", "adult-humor", "adult-how-to",
                "taboo-sex-stories", "interracial-erotic-stories", "lesbian-sex-stories", "erotic-letters","loving-wives", "mature-sex","mind-control", "non-english-stories"
                ,"non-human-stories", "non-erotic-stories", "non-consent-stories", "erotic-novels", "reviews-and-essays","adult-romance","science-fiction-fantasy","masturbation-stories"
                ,"transgender-crossdressers"
        };
        for (String section : param) {
            Runnable runnable =
                    () -> {
                        crollType(section);
                    };
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

    public static void main(String[] args) {
        getConnection();
        HttpGet request = null;
//        searchUsers();
//        crollAuthor();
        crollAllType();
//       crollType("taboo-sex-stories");
//        crollTagPage();
        //crollAuthor();
    }

    private static void crollTagPage() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            //String urlAuthor = "https://www.literotica.com/stories/memberpage.php?uid=3494312&page=submissions";
            //saveStoryOfAuthor(client, urlAuthor);
            String urlTag = "https://tags.literotica.com/lesbian/?page=80";
            savePageTag(client, urlTag);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void crollAuthor() {
        Set<String> authorSet = getAllAuthor();

        authorSet.parallelStream().forEach(value
                        -> {
                    HttpClient client = HttpClientBuilder.create().build();
                    saveStoryOfAuthor(client, "https://www.literotica.com/stories/memberpage.php?uid=" + value + "&page=submissions");
                }
        );
    }

    private static void savePageTag(HttpClient client, String urlTag) throws IOException {
        System.out.println(urlTag);
        String contentHTML = getContentHTMLFRomURL(client, urlTag);
        Document html = Jsoup.parse(contentHTML);
        Elements liens = html.getElementsByClass("W_ir");
        List<String> listUrl = liens.stream().map(td -> getHref(td)).collect(Collectors.toList());
        listUrl.parallelStream().forEach(lien -> {
            saveStory(client, lien);
        });
        Elements nextLink = html.select("a[title=\"Next Page\"]");
        if (nextLink.size() > 0) {
            String lien = getHref(nextLink.get(0));
            String base = "https://tags.literotica.com";
            savePageTag(HttpClientBuilder.create().build(), base + lien);
        }
    }

    private static List<String> idAuthorChecked = new ArrayList<>();

    private static void saveStoryOfAuthor(HttpClient client, String urlAuthor) {
        try {

            String idAuthor = urlAuthor
                    .replace("https://www.literotica.com/stories/memberpage.php?uid=", "")
                    .replace("&page=submissions", "");
            if (doSaveAuthor) {
                synchronized (idAuthorChecked) {
                    if (idAuthorChecked.contains(idAuthor)) {
                        return;
                    }
                    idAuthorChecked.add(idAuthor);
                }
            }
            log.info("checking author : {} ", idAuthor);
            String contentHTML = getContentHTMLFRomURL(client, urlAuthor);
            Document html = Jsoup.parse(contentHTML);
            String author = html.select("a.contactheader").get(0).text();
//            if (doSaveAuthor) {
                if (isAuthorInBase(idAuthor)) {
                   // return;
                } else {
                    insertAuthor(idAuthor, author);
                }
//            }
            if (doSaveStory) {
                html.getElementsByClass("fc")
                        .stream().forEach(td -> {
                    Element linkStory = td.getElementsByClass("bb").get(0);
                    String lien = getHref(linkStory);
                    saveStory(client, lien);
                });
            }
            Elements listTR = html.select("tr.ser-ttl,tr.root-story,tr.sl");
            String serieTitle = "";
            for (Element tr : listTR) {

                int order = 0;
                // On ignore les histoires seule.
                if (tr.hasClass("root-story")) {
                    saveMetaDataStory(tr);
                    continue;
                }
                if (tr.hasClass("ser-ttl")) {
                    serieTitle = tr.text().replaceAll(": .* Part Series", "");
                    order = 0;
                }
                if (tr.hasClass("sl")) {
                    order++;
                    String url = getHref(tr.getElementsByClass("bb").get(0));
                    if (!url.contains("/s/")) {
                        log.info("Ignore link {}", url);
                        continue;
                    }
                    saveMetaDataStory(tr);
                    String key = getKeyStory(url);
                    if (!isSerieLinkInBase(idAuthor, serieTitle, key)) {
                        insertSerieLink(idAuthor, serieTitle, key, order);
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            log.error("Impossible to find author {}", urlAuthor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveMetaDataStory(Element tr) {
        Elements tdStory = tr.getElementsByTag("td");
        Element linkStory = tdStory.get(0).getElementsByClass("bb").get(0);
        String lien = getHref(linkStory);
        if (!lien.contains("/s/")) {
            log.info("Ignore link {}", lien);
            return;
        }
        String key = getKeyStory(lien);
        String description = tdStory.get(1).text();
        String type = tdStory.get(2).text();
        Date date = null;
        try {
            date = formatter.parse(tdStory.get(3).text());
        } catch (ParseException e) {
            log.warn("Erreur lors de l'analyse de la date {}", tdStory.get(3).text());
        } catch (NumberFormatException e) {
            log.warn("Erreur lors de l'analyse de la date {}", tdStory.get(3).text());
        }


        String note = tdStory.get(0).text().replace(linkStory.text(), "")
                .replace("(", "")
                .replace(")", "");
        addInformationStory(key, description, type, date, note);
    }


    private static void saveStory(HttpClient client, String url) {
        if (!url.contains("/s/")) {
            log.info("Ignore {}", url);
            return;
        }
        String key = getKeyStory(url);
        if (isStoryInBase(key)) {
            return;
        }
        try {
            String contentHTML = getContentHTMLFRomURL(client, url);
            Document html = Jsoup.parse(contentHTML);
            String content = html.getElementsByClass("b-story-body-x").get(0).html();
            String author = html.getElementsByClass("b-story-user-y").get(0).text();
            String title = html.getElementsByTag("h1").get(0).text();
            Elements sectionTag = html.getElementsByClass("b-s-story-tag-list");
            while (html.getElementsByClass("b-pager-next").size() > 0) {
                Element aNext = html.getElementsByClass("b-pager-next").get(0);
                String lien = getHref(aNext);
                contentHTML = getContentHTMLFRomURL(client, lien);
                html = Jsoup.parse(contentHTML);
                content += html.getElementsByClass("b-story-body-x").get(0).html();
                sectionTag = html.getElementsByClass("b-s-story-tag-list");
            }
            if (isStoryInBase(key)) {
                return;
            }
            if (sectionTag.size() > 0) {
                Elements linkTag = sectionTag.get(0).getElementsByTag("a");
                List<String> tags = linkTag.stream().map(Element::text).collect(Collectors.toList());
                insertTagStory(key, tags);
            }
            insertStory(key, title, author, content);
        } catch (java.lang.IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getKeyStory(String url) {
        try {
            return url.split("/s/")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error(url, e);
            throw e;
        }
    }

    private static String getHref(Element aNext) {
        return aNext.attr("href");
    }

    private static String getContentHTMLFRomURL(HttpClient client, String url) throws IOException {
        log.info("Fetch content : {}", url);
        HttpGet request;
        request = new HttpGet(url);

        request.addHeader("User-Agent", "Apache HTTPClient");
        HttpResponse response = client.execute(request);

        HttpEntity entity = response.getEntity();
        String toReturn = EntityUtils.toString(entity);
        if (request != null) {
            request.releaseConnection();
        }
        return toReturn;
    }
}
