package em.external.org.zalando;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.h2.tools.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    public static void main(String[] args) {

        int controllerPort = 40100;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = 12345;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/original/catwatch/catwatch-backend/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/catwatch-sut.jar";
        }

        int timeoutSeconds = 120;
        if(args.length > 3){
            timeoutSeconds = Integer.parseInt(args[3]);
        }

        String command = "java";
        if(args.length > 4){
            command = args[4];
        }

        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int timeoutSeconds;
    private final int sutPort;
    private final int dbPort;
    private String jarLocation;
    private Connection connection;
    private Server h2;


    public ExternalEvoMasterController() {
        this(40100, "../core/target", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command) {
        this.sutPort = sutPort;
        this.dbPort = sutPort + 1;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
        setJavaCommand(command);
    }

    private String dbUrl( ) {

        String url = "jdbc";
        url += ":h2:tcp://localhost:" + dbPort + "/./temp/tmp_catwatch/testdb_" + dbPort;

        return url;
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{};
    }

    public String[] getJVMParameters() {
        return new String[]{
                "-Dserver.port=" + sutPort,
//                "-Dspring.datasource.url=" + dbUrl(false) + ";DB_CLOSE_DELAY=-1",
//                "-Dspring.datasource.driver-class-name=" + getDatabaseDriverName(),
                //FIXME: re-enable once fixed issue with Spring
                "-Dspring.datasource.url=" + dbUrl() + ";DB_CLOSE_DELAY=-1",
                "-Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "-Dspring.datasource.username=sa",
                "-Dspring.datasource.password"
        };
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:" + sutPort;
    }

    @Override
    public String getPathToExecutableJar() {
        return jarLocation;
    }

    @Override
    public String getLogMessageOfInitializedServer() {
        return "Started CatWatchBackendApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {

        try {
            //starting H2
            h2 = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "" + dbPort);
            h2.start();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postStart() {
        closeDataBaseConnection();

        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(dbUrl(), "sa", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resetStateOfSUT() {
        DbCleaner.clearDatabase_H2(connection, Arrays.asList("schema_version"));
    }

    @Override
    public void preStop() {
        closeDataBaseConnection();
    }

    @Override
    public void postStop() {
        if (h2 != null) {
            h2.stop();
        }
    }

    private void closeDataBaseConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            connection = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.zalando.";
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/v2/api-docs",
                //TODO /fetch relies on accessing Github, and it is veryyyy slow.
                // Need to handle WireMock
                Arrays.asList("/fetch", "/health", "/health.json", "/error")
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getDatabaseDriverName() {
        return "org.h2.Driver";
    }

 }
