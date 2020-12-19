package org.k.porn;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExtractStory {
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

    public static void main(String[] args) throws SQLException, IOException {
        getConnection();
        String titleSeries= "Daisy''s Disgrace";
        int idAuthor = 3665702;
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("select title, author, content from story where key in (select chapter from series where title ='"+titleSeries+"' and id_author="+idAuthor+")order by key");
        StringBuilder fullStory = new StringBuilder();
        String title = null;
        String author = null;
        while (rs.next()) {
            System.out.println(rs.getString(1));
            title = rs.getString("title");
            author = rs.getString("author");
            fullStory.append("<h1 class=\"chapter\">");
            fullStory.append(rs.getString("title"));
            fullStory.append("</h1>");
            fullStory.append(rs.getString("content"));
        }
        FileWriter myWriter = new FileWriter(titleSeries+".html");
        myWriter.write("<html><head><title>");
        myWriter.write(titleSeries);
        myWriter.write("</title>");
        myWriter.write("<meta content=\""+ author + "\" name=\"author\">");
        myWriter.write("</head><body>");
        myWriter.write(fullStory.toString());
        myWriter.write("</html>");
        myWriter.close();
    }
}
