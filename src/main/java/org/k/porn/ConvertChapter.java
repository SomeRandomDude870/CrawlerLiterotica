package org.k.porn;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class ConvertChapter {
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

    private static List<String> getListKeyChapter() throws SQLException {
        Statement stkey = con.createStatement();
        ResultSet rskey = stkey.executeQuery("select distinct key from story ");
        List<String> toReturn = new ArrayList<>();
        while (rskey.next()) {
            String key = rskey.getString("key");
            toReturn.add(key);
        }
        return toReturn;
    }
    public static void main(String[] args) throws SQLException, IOException {
        getConnection();

        getListKeyChapter().parallelStream().forEach(
        value->{
            try {
                String command  ="\"C:\\Program Files\\Calibre2\\ebook-convert.exe\" \"chapter/"+value+".html\" \"chapterConverted/"+value+".azw3\"";
                Process p = Runtime.getRuntime().exec(command);
                BufferedReader reader=new BufferedReader(new InputStreamReader(
                        p.getInputStream()));
                String line;
                while((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        );



    }
}
