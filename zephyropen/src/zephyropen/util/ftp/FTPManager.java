package developer.ftp;


import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import oculus.Application;
import oculus.Observer;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * Manage FTP configuration and connections. Start new threads for each FTP transaction
 * 
 * @author <a href="mailto:brad.zdanivsky@gmail.com">Brad Zdanivsky</a>
 */
public class FTPManager implements Observer {
	
	private static Logger log = Red5LoggerFactory.getLogger(FTPManager.class, "oculus");
	private State state = State.getReference();
	// private java.util.Timer timer = new Timer();
	private Settings settings = new Settings();
	private boolean configured = true;
	private String folderName = null;
	private String ftpURL = null;
	private String userName = null;
	private String password = null;
	private String port = null;
	private Application app = null;

	/** try to configure FTP parameters */
	public FTPManager(Application a) {
		app = a;
		ftpURL = settings.readSetting("ftp_host");
		userName = settings.readSetting("ftp_user");
		folderName = settings.readSetting("ftp_folder");
		password = settings.readSetting("ftp_password");
		port = settings.readSetting("ftp_port");
	
		if(ftpURL==null)configured = false;
		if(userName==null)configured = false;
		if(folderName==null)configured = false;
		if(ftpURL==null)configured = false;
		if(port==null) port = "21";
		
		if(configured){
		
			// register for state changes
			state.addObserver(this);
			
			// refresh on timer too 
			// timer.scheduleAtFixedRate(new FtptTask(), State.ONE_MINUTE, State.FIVE_MINUTES);
		}
	}
	

	@Override
	public void updated(final String key) {
		
		if(key.equalsIgnoreCase(State.sonar)) return;
		
		System.out.println("_updated in state: " + key + " = " + state.get(key));
		
		String value = state.get(key);
		if(value!=null){
			
			if(value.equalsIgnoreCase(State.user))
				ftpFile("currentuser.php", state.get(State.user).toUpperCase());
			
			if(value.equalsIgnoreCase(State.status))
				ftpFile("status.php", state.get(State.status).toUpperCase());


		}
		
	}
	
	/** FTP given file to host web server */
	private boolean ftpFile(final String fileName, final String data) {

		System.out.println("_ftp: " + fileName);
		
		if(!configured){
			log.error("ftp not configured");
			return false;
		}
		
		FTP ftp = new FTP();

		try {

			ftp.connect(ftpURL, port, userName, password);

		} catch (Exception e) {
			log.equals(e.getMessage());
			log.error("FTP can not connect to: " + ftpURL + " user: " + userName, this);
			return false;
		}

		try {

			if (!ftp.ascii()) {
				log.error("FTP can not switch to ASCII mode", this);
				return false;
			}

			if (!ftp.cwd(folderName)) {
				log.error("FTP can not CD to: " + folderName, this);
				return false;
			}

			if (ftp.storString(fileName, data)){
				app.message("ftp update to: " + ftpURL, null, null);
				return true;
			}
		} catch (Exception e) {
			log.error("FTP upload exception : " + fileName, this);
			return false;
		}

		// error state
		return false;
	}

	/** run on timer
	private class FtptTask extends TimerTask {
		
		@Override
		public void run() {
			
			app.message("ftp update to: " + ftpURL, null, null);

			ftpFile("status.php", state.get(State.status));
			ftpFile("currentuser.php", state.get(State.user));
			ftpFile("connectedlast.php", state.get(State.boottime)); 
			ftpFile("boottime.php", state.get(State.logintime));
		
			ftpFile("guesthours.php", settings.getBoolean(State.guest_start) 
					+ ":00 hours until " + settings.readSetting(State.guest_end) + ":00 hours");
		
			ftpFile("address.php", "http://" + Util.getExternalIPAddress() 
					+ ":" + settings.readRed5Setting("http.port") + "/oculus/");

		}
	}*/
}