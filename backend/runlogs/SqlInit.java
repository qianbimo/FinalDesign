import java.nio.file.*;
import java.sql.*;

public class SqlInit {
  public static void main(String[] args) throws Exception {
    String rootUrl = "jdbc:mysql://localhost:3306/mysql?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
    try (Connection conn = DriverManager.getConnection(rootUrl, "root", "root")) {
      try (Statement st = conn.createStatement()) {
        st.execute("CREATE DATABASE IF NOT EXISTS lung_nodule DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
      }
    }

    String dbUrl = "jdbc:mysql://localhost:3306/lung_nodule?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
    String sql = Files.readString(Path.of("E:/code/FinalDesign/backend/src/main/resources/sql/lung_nodule_schema.sql"));
    sql = sql.replace("\uFEFF", "").replace("\r", "");

    try (Connection conn = DriverManager.getConnection(dbUrl, "root", "root")) {
      for (String raw : sql.split(";\\n")) {
        String stmt = raw.trim();
        if (stmt.isEmpty()) continue;
        if (stmt.startsWith("--")) continue;
        if (stmt.toUpperCase().startsWith("CREATE DATABASE")) continue;
        if (stmt.toUpperCase().startsWith("USE ")) continue;
        try (Statement st = conn.createStatement()) {
          st.execute(stmt);
        }
      }
    }
    System.out.println("SQL init done");
  }
}