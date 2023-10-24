package post;

import java.sql.*;
import java.util.List;
import java.util.Vector;

import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;

public class PostDao {
    /*
     * Field
     */

    private Connection conn;
    private PreparedStatement pstmt;
    private ResultSet rs;
    private DataSource ds;

    /*
     * Constructor
     */

    // Default Constructor
    public PostDao() {
        try {
            Context ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/myoracle");
        } catch (Exception e) {
            System.out.println("BoardDao() : " + e);
        }
    }

    /*
     * Method
     */

    // freeConnection
    public void freeConnection() {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // list.jsp, 검색기능
    public Vector<PostDto> getPostList(String option, String searchWord, int board_id) {
        // Board ID 설정 바꾸기 후기게시판 board_id =2 로 임시지정
        String sql = "SELECT post_id, user_id, user_nickname, post_title, "
                + "post_content, post_create_date, post_update_date, post_views "
                + "FROM tblpost WHERE board_id=?";
        if (searchWord == null || searchWord.isEmpty()) {
            sql = sql + "ORDER BY post_id desc";
        } else {
            switch (option) {
                case "title":
                    sql = sql + "AND post_title Like '%" + searchWord + "%' ORDER BY post_id desc";
                    break;
                case "content":
                    sql = sql + "AND post_content Like '%" + searchWord
                            + "%' ORDER BY post_id desc";
                    break;
                case "title+content":
                    sql = sql + "AND post_title Like '%" + searchWord
                            + "%' AND post_content Like '%" + searchWord
                            + "%' ORDER BY post_id desc";
                    break;
                case "nickname":
                    sql = sql + "AND user_nickname Like '%" + searchWord
                            + "%' ORDER BY post_id desc";
                    break;
            }
        }
        Vector<PostDto> vector = new Vector<PostDto>();
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, board_id);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PostDto dto = new PostDto();
                dto.setPost_id(rs.getInt("post_id"));
                dto.setUser_id(rs.getString("user_id"));
                dto.setUser_nickname(rs.getString("user_nickname"));
                dto.setPost_title(rs.getString("post_title"));
                dto.setPost_content(rs.getString("post_content"));
                dto.setPost_create_date(rs.getDate("post_create_date"));
                dto.setPost_views(rs.getInt("post_views"));
                vector.add(dto);
            }
        } catch (Exception e) {
            System.out.println("getPostList() : " + e);
        } finally {
            freeConnection();
        }
        return vector;
    }

    // detail.jsp, update.jsp
    public PostDto getPost(int post_id) {
        String sql = "select * from tblpost where post_id=?";
        PostDto postDto = new PostDto();
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, post_id);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                postDto.setPost_id(post_id);
                postDto.setPost_title(rs.getString("post_title"));
                postDto.setPost_views(rs.getInt("post_views"));
                postDto.setUser_id(rs.getString("user_id"));
                postDto.setUser_nickname(rs.getString("user_nickname"));
                postDto.setPost_create_date(rs.getDate("post_create_date"));
                postDto.setPost_content(rs.getString("post_content"));
                postDto.setBoard_id(rs.getInt("board_id"));
            }
        } catch (SQLException e) {
            System.out.println("getPost() : " + e);
        } finally {
            freeConnection();
        }
        return postDto;
    }

    // postproc.jsp
    public void setPost(PostDto postDto, int board_id) {
        String sql =
                "insert into tblpost(post_id, board_id, user_id, user_nickname, post_title, post_content) "
                        + "values(post_id_seq.nextval, ?, ?, ?, ?, ?)";
        // date 및 views는 db에서 default값으로 설정
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postDto.getBoard_id());
            pstmt.setString(2, postDto.getUser_id());
            pstmt.setString(3, postDto.getUser_nickname());
            pstmt.setString(4, postDto.getPost_title());
            pstmt.setString(5, postDto.getPost_content());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("setPost() : " + e);
        } finally {
            freeConnection();
        }
    }

    // updateproc.jsp
    public void updatePost(PostDto postDto) {
        // 수정하기 버튼이 자기 글에서만 뜨게 수정 후 다시보기
        String sql =
                "UPDATE tblpost SET post_title=?, post_content=?, post_update_date=sysdate WHERE post_id=?";
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, postDto.getPost_title());
            pstmt.setString(2, postDto.getPost_content());
            pstmt.setInt(3, postDto.getPost_id());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("updatePost() : " + e);
        } finally {
            freeConnection();
        }
    }

    // delete.jsp
    public void deletePost(PostDto dto) {
        // 삭제하기 버튼이 자기 글에서만 뜨게 수정 후 다시보기
        String sql = "DELETE FROM tblpost WHERE post_id=?";
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, dto.getPost_id());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("deletePost() : " + e);
        } finally {
            freeConnection();
        }
    }

    // 조회수 쿠키 확인
    public Cookie checkCookie(Cookie[] cookies, int post_id) {
        Cookie viewCookie = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals("|" + post_id + "|")) {
                    viewCookie = cookies[i];
                }
            }
        } else {
            viewCookie = null;
        }
        return viewCookie;
    }

    // 조회수 업데이트
    public void viewsUpdate(int post_id) {
        String sql = "UPDATE tblpost SET post_views = post_views + 1 WHERE post_id=?";
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, post_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("viewsUpdate() : " + e);
        } finally {
            freeConnection();
        }
    }

    public Vector<PostDto> getMyHistory(String user_id) {
        // Board ID 설정 바꾸기 후기게시판 board_id =2 로 임시지정
        String sql = "SELECT * FROM tblpost WHERE user_id=? ORDER BY post_id desc";
        Vector<PostDto> vector = new Vector<PostDto>();
        try {
            conn = ds.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user_id);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PostDto postDto = new PostDto();
                postDto.setPost_id(rs.getInt("post_id"));
                postDto.setUser_id(rs.getString("user_id"));
                postDto.setUser_nickname(rs.getString("user_nickname"));
                postDto.setPost_title(rs.getString("post_title"));
                postDto.setPost_content(rs.getString("post_content"));
                postDto.setPost_create_date(rs.getDate("post_create_date"));
                postDto.setPost_views(rs.getInt("post_views"));
                vector.add(postDto);
            }
        } catch (Exception e) {
            System.out.println("getMyHistory() : " + e);
        } finally {
            freeConnection();
        }
        return vector;
    }

}
