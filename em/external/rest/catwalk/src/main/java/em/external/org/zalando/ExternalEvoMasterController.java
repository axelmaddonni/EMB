package em.external.org.zalando;

import com.p6spy.engine.spy.P6SpyDriver;
import org.evomaster.clientJava.controller.ExternalSutController;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;
import org.evomaster.clientJava.controller.db.DbCleaner;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
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
        jarLocation += "/catwatch-backend.jar";
        int timeoutSeconds = 120;
        if(args.length > 3){
            timeoutSeconds = Integer.parseInt(args[3]);
        }

        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int timeoutSeconds;
    private final int sutPort;
    private final int dbPort;
    private final String jarLocation;
    private Connection connection;
    private Server h2;

    public ExternalEvoMasterController() {
        this(40100, "../core/target", 12345, 120);
    }

    public ExternalEvoMasterController(int controllerPort, String jarLocation, int sutPort, int timeoutSeconds) {
        this.sutPort = sutPort;
        this.dbPort = sutPort + 1;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
    }

    private String dbUrl(boolean withP6Spy) {

        String url = "jdbc";
        if (withP6Spy) {
            url += ":p6spy";
        }
        url += ":h2:tcp://localhost:" + dbPort + "/./temp/tmp_catwatch/testdb_" + dbPort;

        return url;
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{"--server.port=" + sutPort};
    }

    public String[] getJVMParameters() {
        return new String[]{
                "-Dspring.datasource.url=" + dbUrl(true) + ";DB_CLOSE_DELAY=-1",
                "-Dspring.datasource.driver-class-name=" + P6SpyDriver.class.getName(),
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
        return "Started Application in ";
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
            connection = DriverManager.getConnection(dbUrl(false), "sa", "");
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
    public String getUrlOfSwaggerJSON() {
        return getBaseURL() + "/v2/api-docs";
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

    @Override
    public List<String> getEndpointsToSkip() {
        return Arrays.asList("/fetch");
    }
}
