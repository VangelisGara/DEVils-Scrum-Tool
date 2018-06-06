package ys09.data;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import ys09.model.*;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.jdbc.core.PreparedStatementSetter;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;



public class DataAccess {

    private static final int MAX_TOTAL_CONNECTIONS = 16;
    private static final int MAX_IDLE_CONNECTIONS = 8;

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;


    public void setup(String driverClass, String url, String user, String pass) throws SQLException {

        //initialize the data source
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(driverClass);
        bds.setUrl(url);
        bds.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        bds.setMaxIdle(MAX_IDLE_CONNECTIONS);
        bds.setUsername(user);
        bds.setPassword(pass);
        bds.setValidationQuery("SELECT 1");
        bds.setTestOnBorrow(true);
        bds.setDefaultAutoCommit(true);

        //check that everything works OK
        bds.getConnection().close();

        //initialize the jdbc template utilitiy
        jdbcTemplate = new JdbcTemplate(bds);

        //keep the dataSource for the low-level manual example to function (not actually required)
        dataSource = bds;
    }



    public Project insertProject(Project project, int idUser, String role) {
        // Insert an object to the projects array

        String query1 = "insert into Project (title, isDone, deadlineDate) values (?, ?, ?)";
        String query2 = "insert into Project_has_User (Project_id, User_id, role) values (?, ?, ?)";
        // Id is generated by database (Auto Increment)
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            //jdbcTemplate.update(query1, new Object[]{project.getTitle(), project.getIsDone(), project.getDeadlineDate()});
            KeyHolder keyHolder = new GeneratedKeyHolder();
            java.sql.Date sqlDate = new java.sql.Date(project.getDeadlineDate().getTime());

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query1, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, project.getTitle());
                    statement.setBoolean(2, project.getIsDone());
                    statement.setDate(3, sqlDate);
                    return statement;
                }
            }, keyHolder);
            // Return the new generated id for user
            int idProject = keyHolder.getKey().intValue();
            System.out.println(idProject);
            project.setId(idProject);
            project.setDeadlineDate(sqlDate);

            jdbcTemplate.update(query2, new Object[]{idProject, idUser, role});
            return project;
          // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }



    public List<Project> getProjects() {
        //TODO: Support limits SOS
        // Creates the id
        return jdbcTemplate.query("select * from Project", new ProjectRowMapper());
    }


    public List<Project> getUserProjects(int idUser, Limits limit, Boolean isDone){
        // Return all the projects belong to user with the above id

        List<Project> projects = new ArrayList<>();
        String query = "select * from Project where idProject in (select Project_id from Project_has_User where User_id = :id) and isDone = :done and deadlineDate >= :expDate order by deadlineDate limit :limit";
        // Use NamedParameterJdbcTemplate because of Date parsing
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", idUser);
        params.addValue("limit",  limit.getCount());
        params.addValue("expDate", limit.getStart(), Types.DATE);
        params.addValue("done", isDone);
        try {
            return namedJdbcTemplate.query(query, params, new ProjectRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return projects;
        }
    }



    public Project getCurrentProject(int projectId){
        // Get current project Information
        String query = "select * from Project where idProject = ?;";
        Project projectItem = jdbcTemplate.queryForObject(query, new Object[]{projectId}, new ProjectRowMapper());
        return projectItem;
    }



    // Find the PBIs (Epics or Stories)
    public List<PBI> getProjectPBIs(int idProject, Boolean isEpic, int epicId) {
        // Return the pbis asked for the current project
        //System.out.println(isEpic);
        List<PBI> pbis = new ArrayList<>();
        if (isEpic == true) {
            // Returns Epics
            String query = "select * from PBI where Project_id = :idProject and isEpic = :isEpic";
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("idProject", idProject);
            params.addValue("isEpic", isEpic);
            try {
                return namedJdbcTemplate.query(query, params, new PBIRowMapper());
            } catch(EmptyResultDataAccessException e) {
                return pbis;
            }
        }         // Returns User Stories on else
        else {
            String query = "select * from PBI where Project_id = :idProject and Epic_id = :epicId";
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("idProject", idProject);
            params.addValue("epicId", epicId);
            try {
                return namedJdbcTemplate.query(query, params, new PBIRowMapper());
            } catch(EmptyResultDataAccessException e) {
                return pbis;
            }
        }

    }


    public PBI insertNewPBI(PBI pbi) {
        // Insert a pbi into PBI table
        String query = "insert into PBI (title, description, priority, isEpic, Project_id, Epic_id, Sprint_id) values (?, ?, ?, ?, ?, ?, ?);";
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Id is generated by database (Auto Increment)
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, pbi.getTitle());
                    statement.setString(2, pbi.getDescription());
                    statement.setInt(3, pbi.getPriority());
                    statement.setBoolean(4, pbi.getIsEpic());
                    statement.setInt(5, pbi.getProject_id());
                    if (pbi.getEpic_id() != null)
                        statement.setInt(6, pbi.getEpic_id());
                    else statement.setNull(6, java.sql.Types.INTEGER);
                    if (pbi.getSprint_id() != null)
                        statement.setInt(7, pbi.getSprint_id());
                    else statement.setNull(7, java.sql.Types.INTEGER);
                    return statement;
                }
            }, keyHolder);

            // Return the new generated id for pbi
            int idPBI = keyHolder.getKey().intValue();
            System.out.println(idPBI);
            pbi.setIdPBI(idPBI);
            return pbi;

        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }



    public PBI updatePBI(PBI pbi) {
        // Update an existing PBI
        String query = "update PBI set title=?, description=?, priority=?, isEpic=?, Project_id=?, Epic_id=?, Sprint_id=? where idPBI=?;";
        jdbcTemplate = new JdbcTemplate(dataSource);

        try {
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, pbi.getTitle());
                    statement.setString(2, pbi.getDescription());
                    statement.setInt(3, pbi.getPriority());
                    statement.setBoolean(4, pbi.getIsEpic());
                    statement.setInt(5, pbi.getProject_id());
                    if (pbi.getEpic_id() != null)
                        statement.setInt(6, pbi.getEpic_id());
                    else statement.setNull(6, java.sql.Types.INTEGER);
                    if (pbi.getSprint_id() != null)
                        statement.setInt(7, pbi.getSprint_id());
                    else statement.setNull(7, java.sql.Types.INTEGER);
                    statement.setInt(8, pbi.getIdPBI());
                    return statement;
                }
            });
            // PBI's id is already in pbi class (as returned from frontend)
            return pbi;
        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }


    // Update PBI's Sprint_id field
    public void updateSprintId(List<PBI> pbis) {

        String sql = "UPDATE PBI SET Sprint_id = ? WHERE idPBI = ?";
        JdbcTemplate template = new JdbcTemplate(dataSource);
        System.out.println("updateSprint");

        template.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PBI singlePbi = pbis.get(i);
                ps.setInt(1, singlePbi.getSprint_id());
                ps.setInt(2, singlePbi.getIdPBI());
            }
            @Override
            public int getBatchSize() {
                return pbis.size();
            }
        });
    }


    
    public Sprint getProjectCurrentSprint(int projectId) {
        // Get the current sprint of a project
        String query = "select * from Sprint where Project_id = ? and isCurrent = TRUE;";
        try {
            Sprint sprintItem = jdbcTemplate.queryForObject(query, new Object[]{projectId}, new SprintRowMapper());
            return sprintItem;
        } catch (EmptyResultDataAccessException e) {
            Sprint sprintItem = new Sprint();
            return sprintItem;
        } 
    }


    // Create new sprint
    // Insert User
    public int createNewSprint(Sprint sprint) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        java.sql.Date sqlDate = new java.sql.Date(sprint.getDeadlineDate().getTime());

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement statement = con.prepareStatement("INSERT INTO Sprint(deadlineDate, goal, plan, isCurrent, numSprint, Project_id) VALUES (?, ?, ?, ?, ?, ?) ", Statement.RETURN_GENERATED_KEYS);
                statement.setDate(1, sqlDate);
                statement.setString(2, sprint.getGoal());
                statement.setString(3, sprint.getPlan());
                statement.setBoolean(4, sprint.getCurrent());
                statement.setInt(5, sprint.getNumSprint());
                statement.setInt(6, sprint.getProject_id());
                return statement;
            }
        }, keyHolder);
        // Return the new generated id for user
        return keyHolder.getKey().intValue();
    }


    // Check if User exists into the database
    public boolean userExists(String mail) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);
        String query = "SELECT * FROM User WHERE mail = ?";

        try {
            User exist = jdbcTemplate.queryForObject(query, new Object[]{mail}, new UserRowMapper());   // Exists
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }        // Does not exists
    }


    // Insert User
    public int insertUser(User user) {
        // Generate Random Salt and Bcrypt
        String pw_hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(pw_hash);
        user.setIsAdmin(0);
        user.setNumOfProjects(0);
        // Insert into table with jdbc template
        // Avoid SQL injections
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement statement = con.prepareStatement("INSERT INTO User(mail, firstname, lastname, password, isAdmin, numProjects) VALUES (?, ?, ?, ?, ?, ?) ", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, user.getEmail());
                statement.setString(2, user.getFirstName());
                statement.setString(3, user.getLastName());
                statement.setString(4, user.getPassword());
                statement.setInt(5, user.getIsAdmin());
                statement.setInt(6, user.getNumOfProjects());
                return statement;
            }
        }, keyHolder);
        // Return the new generated id for user
        return keyHolder.getKey().intValue();
    }



    public List<Task> getSprintTasks(int sprintId) {
        // Find the Tasks belong to a specific Sprint
        String query = "select * from Task where PBI_id in (select idPBI from PBI where Sprint_id = ?);";
        return jdbcTemplate.query(query, new Object[]{sprintId}, new TaskRowMapper());
    }



    public List<Issue> getSprintIssues(int sprintId) {
        // Find the Issues of current sprint's tasks
        String query = "select * from Issue where Task_id in (select idTask from Task where PBI_id in (select idPBI from PBI where Sprint_id = ?));";
        return jdbcTemplate.query(query, new Object[]{sprintId}, new IssueRowMapper());
    }



    public List<Team> getTeamMembers(int projectId) {
        // Find the Members of current project
        String query = "select idUser, mail, firstname, lastname, photo from User where idUser in (select User_id from Project_has_User where Project_id = ?);";
        return jdbcTemplate.query(query, new Object[]{projectId}, new TeamRowMapper());
    }



    public String getMemberRole(int userId, int projectId) {
        // Find the role of a specific member in project
        String query = "select role from Project_has_User where User_id = ? and Project_id = ?;";
        return jdbcTemplate.queryForObject(query, new Object[]{userId, projectId}, String.class);
    }



    public Team insertNewMember(Team member, int projectId) {
        // Insert a new member into project (update the Project_has_User table)
        String query = "insert into Project_has_User (Project_id, User_id, role) values (?, ?, ?);";

        // Take the user's id
        String queryInfo = "select * from User where mail = ?;";
        User userInfo = jdbcTemplate.queryForObject(queryInfo, new Object[]{member.getMail()}, new UserRowMapper());
        int userId = userInfo.getId();
        System.out.println(userId);

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement statement = con.prepareStatement(query);
                statement.setInt(1, projectId);
                statement.setInt(2, userId);
                statement.setString(3, member.getRole());;
                return statement;
            }
        });
        // Return member information
        member.setIdUser(userId);
        member.setFirstname(userInfo.getFirstName());
        member.setLastname(userInfo.getLastName());
        member.setPhoto(userInfo.getPhoto());
        return member;
    }



    public int checkSignIn(SignIn signin) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);
        String query = "SELECT * FROM User WHERE mail = ?";
        String mail = signin.getEmail();
        try {
            User user = jdbcTemplate.queryForObject(query, new Object[]{mail}, new UserRowMapper());

            if (BCrypt.checkpw(signin.getPassword(), user.getPassword())) {
                System.out.println("It matches");
                // If it matches return JWT token !
                // Save the token to a dictionary (user,token)
                return user.getId();
            } else {
                System.out.println("It does not match");
                return 0;
            }
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    // Check User Email and Password

    public List<Project> getUserProjectsRole (int idUser, String role) {
        // Return all the projects belong to user with the above id

        List<Project> projects = new ArrayList<>();
        String query = "select * from Project where idProject in (select Project_id from Project_has_User where User_id = ? and Project_has_User.role=?)";
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            return jdbcTemplate.query(query, new Object[]{idUser, role}, new ProjectRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return projects;
        }
    }

    public List<Integer> createAuthProjectList (int id, String role)
    {
        List<Project> projectsByRole = getUserProjectsRole(id,role);
        List<Integer> projectsByRoleID = new ArrayList<Integer>();
        for (Project project: projectsByRole)
        {
            projectsByRoleID.add(project.getId());   //make list with the ids' of projects in which
            //the current user is owner
        }
        return projectsByRoleID;
    }



}
