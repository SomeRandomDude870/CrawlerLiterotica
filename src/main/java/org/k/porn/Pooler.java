package org.k.porn;

import java.io.IOException;

public class Pooler {

    public static void main(String[] args) {
        String[] param= {"anal-sex-stories","bdsm-stories", "celebrity-stories", "erotic-couplings",
            "erotic-horror", "exhibitionist-voyeur", "fetish-stories", "first-time-sex-stories", "gay-sex-stories", "group-sex-stories", "adult-how-to",
        "adult-humor", "illustrated-erotic-fiction", "taboo-sex-stories", "interracial-erotic-stories", "lesbian-sex-stories",
        "erotic-letters", "loving-wives", "mature-sex", "mind-control", "non-erotic-stories", "non-consent-stories", "non-human-stories",
        "erotic-novels", "reviews-and-essays", "adult-romance", "science-fiction-fantasy", "transgender-crossdressers"};
        for (String section:param) {
            Runnable runnable =
                    () -> { System.out.println("Lambda Runnable running"); };
        }

    }
}
