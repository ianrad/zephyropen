package zephyropen.util.ftp;

import java.io.FileInputStream;
import java.util.Properties;

import zephyropen.api.ZephyrOpen;
import zephyropen.util.LogManager;
import zephyropen.util.google.GoogleChart;

/**
 * Manage FTP configuration and connections. Start new threads for each FTP transaction
 * 
 * @author <a href="mailto:brad.zdanivsky@gmail.com">Brad Zdanivsky</a>
 */
public class FTPManager { 
	
	private final ZephyrOpen constants = ZephyrOpen.getReference();
	
	private final String FTP_PROPERTIES = "ftp.properties";

	private static FTPManager singleton = null;
    
    private int port = 21;

    private String folderName = null;

    private String ftpURL = null;

    private String userName = null;

    private String password = null;
    
    private boolean configured = false;
    
    private LogManager log = null;
    
    
    /** @return a reference to this singleton class */
    public static FTPManager getReference() {
        if (singleton == null) {
           singleton = new FTPManager();
        }
        return singleton;
    }
    
    /** try to configure FTP parameters */ 
    private FTPManager() {
    	
    	// try to read from file if not in constants 
    	if( ! constantsConfigured()){
    		if(parseFile(constants.get(ZephyrOpen.userHome))){
    			
    			configured = true;
    			
    		} else {
    			
    			// try root if nothing else 
    			configured = parseFile(constants.get(ZephyrOpen.root));		
    		}
    	}
    	
    	if(configured && constants.getBoolean(ZephyrOpen.frameworkDebug)){
    		log = new LogManager();
    		log.open(constants.get(ZephyrOpen.userLog) + ZephyrOpen.fs + constants.get(ZephyrOpen.deviceName) + "_ftp.log");
    	}
	}

    /** Create an ftp thread to send this google report to the hosted server */
    public void upload(GoogleChart report) {
    	
    	// constants.info("ftp thread called: " + report.getTitle(), this);
    	
    	if( ! configured){
    		// System.err.println("ftp not configured");
    		return;
    	}
    	
    	if( ! constants.getBoolean(ZephyrOpen.ftpEnabled)){
    		// System.err.println("ftp not enabled");
    		return;
    	}
    	
    	if( ! constants.getBoolean(ZephyrOpen.filelock)){
    		// System.err.println("do not have the lock file");
    		// System.out.println(constants.toString());
    		return;
    	}
    	
    	String data = report.getURLString();
    	if( data != null){
    		
    		new ftpThread(data, report.getTitle() + ".php").start();
    		
    		if(log!=null)
    			log.append(data);
    	}
    }

	/** FTP given file to host web server */
    private boolean ftpFile(String data, String fileName) {

    	FTP ftp = new FTP();
    	
        try {

            ftp.connect(ftpURL, port, userName, password);

        } catch (Exception e) {
            constants.error(e.toString());
            constants.error("FTP can not connect to: " + ftpURL + " user: " + userName, this);
            return false;
        }
        
        // constants.info("ftp name: " + fileName);
        
        try {

             if (!ftp.ascii()) {
                constants.error("FTP can not switch to ASCII mode", this);
                return false;
            }

            if (ftp.cwd(folderName))
                if (ftp.storString(fileName, data))
                    return true;
            
        } catch (Exception e) {
            constants.error("FTP upload exception : " + fileName, this);
            return false;
        }

        // error state
        return false;
    }

    /** test if the configuration supports ftp */
    public boolean constantsConfigured() {
        
        int test = constants.getInteger(ZephyrOpen.ftpPort);
        if (test != ZephyrOpen.ERROR)
        	port = test;

        ftpURL = constants.get(ZephyrOpen.host);
        if (ftpURL == null) 
            return false;
        
        userName = constants.get(ZephyrOpen.ftpUser);
        if (userName == null)
            return false;

        folderName = constants.get(ZephyrOpen.folder);
        if (folderName == null)
            return false;
    
        password = constants.get(ZephyrOpen.password);
        if (password == null) 
            return false;
        
        // all set
        return true;
    }
    
    /** try reading in defaults, add to constants */ 
    private boolean parseFile(String path){
    	
    	path += ZephyrOpen.fs + FTP_PROPERTIES;
    	
    	Properties props = new Properties();
    	
    	try {

			// set up new properties object from file
			FileInputStream propFile = new FileInputStream(path);
			props.load(propFile);
			propFile.close();
	  
			int test = Integer.parseInt(props.getProperty(ZephyrOpen.ftpPort));
			if (test != ZephyrOpen.ERROR)
	        	port = test;
			
			ftpURL = props.getProperty(ZephyrOpen.host);
	        if (ftpURL == null) 
	            return false;
	        
			userName = props.getProperty(ZephyrOpen.ftpUser);
	        if (userName == null)
	            return false;
	        
	        folderName = props.getProperty(ZephyrOpen.folder);
	        if (folderName == null)
	            return false;
	        
	        password = props.getProperty(ZephyrOpen.password);
	        if (password == null) 
	            return false;
	        
		} catch (Exception e) {
			System.err.println("can't parse FTP config file: " + path);
			return false;
		}	

		// all set
	    return true;
    }

    /** is the ftp configuration valid */
	public boolean ftpConfigured() {
		return configured;
	}
	
	/** do the work */
	private class ftpThread extends Thread {
		
		private String d = null;
		private String f = null;
		
		public ftpThread(String value, String name){
			d = value;
			f = name;
		}

	    @Override
	    public void run() {
	        if ( ! ftpFile(d, f))
	        	constants.error("ftp failed: " + f);
	    }
	}
}