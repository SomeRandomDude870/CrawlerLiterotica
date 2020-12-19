package org.k.porn;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ExtractLink {
    public static Connection con = null;

    public static void getConnection() {
        String url = "jdbc:postgresql://localhost/literotica";
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "admin");
        //props.setProperty("ssl","true");
        try {
            Connection connection = DriverManager.getConnection(url, props);
            con = connection;
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT VERSION()");
            if (rs.next()) {
                //System.out.println(rs.getString(1));
            }

        } catch (SQLException ex) {

            Logger lgr = Logger.getLogger(Main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    public static void main(String[] args) {
        getConnection();
        Pattern p = Pattern.compile(" src=\"/images/([^\"]*)\\\"");
        try {
            PreparedStatement ps = con.prepareStatement("select key, content from story where content like '%src%'");
            ResultSet rs = ps.executeQuery();
            Set<String> groupImg = new HashSet<>();
            while (rs.next()) {

                List<String> urls = new ArrayList<>();
                Matcher m =p.matcher(rs.getString("content"));
                while(m.find()) {
                    urls.add(m.group(1));
                }
                log.info("Key : {} -> {}", rs.getString("key"), urls);
                groupImg.addAll(urls);
            }

            groupImg.parallelStream().forEach( link -> {
                        try (InputStream in = new URL("https://www.literotica.com/images/" + link).openStream()) {
                            Path target = Paths.get("C:\\Users\\K\\IdeaProjects\\literotica_Downloader\\test\\images" + link);
                            target.getParent().toFile().mkdirs();
                            Files.copy(in, target);
                        } catch (Exception e) {
                            log.error("Fail to save", e);
                        }
                    }
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
