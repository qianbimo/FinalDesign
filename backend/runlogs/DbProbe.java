import java.sql.*;
import java.util.*;

public class DbProbe {
  public static void main(String[] args) {
    String host = "jdbc:mysql://localhost:3306/mysql?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
    List<String[]> creds = List.of(
      new String[]{"root","root"},
      new String[]{"root","123456"},
      new String[]{"root",""},
      new String[]{"admin","admin"},
      new String[]{"admin","123456"},
      new String[]{"lung","lung"}
    );
    for (String[] c : creds) {
      try (Connection conn = DriverManager.getConnection(host, c[0], c[1])) {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("select user(), current_user(), version()")) {
          rs.next();
          System.out.println("OK " + c[0] + "/" + c[1] + " => user=" + rs.getString(1) + ", current=" + rs.getString(2) + ", version=" + rs.getString(3));
        }
        return;
      } catch (Exception e) {
        System.out.println("FAIL " + c[0] + "/" + c[1] + " => " + e.getMessage());
      }
    }
  }
}