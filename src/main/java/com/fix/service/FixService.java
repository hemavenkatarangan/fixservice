package com.fix.service;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Initiator;
import quickfix.InvalidMessage;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RejectLogon;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.StringField;
import quickfix.UnsupportedMessageType;
import quickfix.field.MsgType;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.fix44.Heartbeat;

@Component
public class FixService  implements Application {

	private static Initiator initiator = null;
	private static SessionSettings settings;
	Logger logger = Logger.getLogger(FixService.class.getName());
	
	private boolean isAvailable = true;
	

	private WatchService watcher = null;
	private Map<WatchKey, Path> keys = null;
	private boolean recursive = false;
	private boolean trace = false;
	 private ObservableLogon observableLogon = new ObservableLogon();
	private boolean initiatorStarted = false;
	private SessionID sessionID;
	private String dataDictionaryFile="";
	private Path fileName;


	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	public FixService()
	{
		
	}
	

	@Override
	public void onCreate(SessionID sessionId) {
		// TODO Auto-generated method stub

	}

	public void onLogon(SessionID sessionID) {
		this.sessionID=sessionID;
		observableLogon.logon(sessionID);
	}

	public void onLogout(SessionID sessionID) {
		observableLogon.logoff(sessionID);
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		// TODO Auto-generated method stub

	}
	public class MessageProcessor {
        private quickfix.Message message;
        private SessionID sessionID;

        public MessageProcessor(quickfix.Message message, SessionID sessionID) {
            this.message = message;
            this.sessionID = sessionID;
           
        }

        public void processMessage() {
            try {
                MsgType msgType = new MsgType();
                if (isAvailable) {

                     if (message.getHeader().getField(msgType).valueEquals("8")) {
                        executionReport(message, sessionID);
                     }
                    
                } 
            } catch (Exception e) {
                System.out.println(e);
            }

        }
    }
	
	private void executionReport(Message message, SessionID sessionID) throws FieldNotFound {

    }
	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		
		if(message instanceof Heartbeat)
		{
			Heartbeat incoming=(Heartbeat) message;
			OrderID orderID = new OrderID();
			
		}
		
		
		new MessageProcessor(message, sessionId).processMessage();
	}

	
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		System.out.println("Message to be sent  and sessionid "+message);
	}
	
     @Override
	 public void fromApp(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
     IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    	// Creating an instance of file
    	 Path path = null;
    			
    				
    			
		 System.out.println("Message recd  "+message);
		 String ordStatus = message.getString(OrdStatus.FIELD);		
		 System.out.println(ordStatus +" status ");
		 if(ordStatus.equals("0"))
		 {
			 try {
				path = Paths.get(settings.getString("TapOutputFilePath")+File.separator+"acknowledgement_"+fileName.toString());
			} catch (ConfigError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				System.out.println("Output file will be written to "+path.toString());
		 }
		 if(ordStatus.equals("2"))
		 {
			 try {
				path = Paths.get(settings.getString("TapOutputFilePath")+File.separator+"filled_"+fileName.toString());
			} catch (ConfigError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				System.out.println("Output file will be written to "+path.toString());
		 }
		 if(ordStatus.equals("A"))
		 {
			 try {
				path = Paths.get(settings.getString("TapOutputFilePath")+File.separator+"pendingnew_"+fileName.toString());
			} catch (ConfigError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				System.out.println("Output file will be written to "+path.toString());
		 }  
		 if(ordStatus.equals("3"))
		 {
			 try {
				path = Paths.get(settings.getString("TapOutputFilePath")+File.separator+"donefortheday_"+fileName.toString());
			} catch (ConfigError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				System.out.println("Output file will be written to "+path.toString());
		 }  
	        // Try block to check for exceptions
	        try {
	            // Now calling Files.writeString() method
	            // with path , content & standard charsets
	        	byte[] strToBytes = message.toRawString().getBytes();

	            Files.write(path, strToBytes);
	        		           
	        }
	 
	        // Catch block to handle the exception
	        catch (IOException ex) {
	            // Print messqage exception occurred as
	            // invalid. directory local path is passed
	            System.out.print("Invalid Path");
	        }
		    

}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the
	 * WatchService.
	 */
	private void registerAll(final Path start) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Creates a WatchService and registers the given directory
	 */
	FixService(Path dir, boolean recursive) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = recursive;

		if (recursive) {
			System.out.format("Scanning %s ...\n", dir);
			registerAll(dir);
			System.out.println("Done.");
		} else {
			register(dir);
		}

		// enable trace after initial registration
		this.trace = true;
	}

	/**
	 * Process all events for keys queued to the watcher
	 * @throws ConfigError 
	 */
	void processEvents() throws ConfigError {
		for (;;) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();

				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);

				// print out event
				System.out.format("%s: %s\n", event.kind().name(), child);
				if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
					System.out.println("New/Updated file exists.. so now scanning.....");
					dataDictionaryFile=settings.getString("DataDictioneryFile");
					System.out.println("DAta dictionary file "+dataDictionaryFile);
					fileName=child.getFileName();
					try {
						InputStream fileInputStream = Files.newInputStream(child, StandardOpenOption.READ);
						String msg = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);

						CustomNewOrderSingle single = new CustomNewOrderSingle();
						try {
							// String	 msg="8=FIX.4.4^9=81^35=D^11=1663159934929^21=1^38=123^40=1^54=1^55=ibm^59=0^60=20220914-12:52:20.730^10=131^";
							// String
							// msg1="8=FIX.4.4^9=222^35=D^34=999^49=TAPWS^52=20201106-15:29:49.000^56=COUNTERPARTY^11=D1903001055^15=USD^22=4^38=40^40=1^44=191.65^48=US5949181045^54=1^55=MSFT^59=0^60=20201106-15:29:27.000^100=XNYS^461=EXXXX^22222=D1903001055;29004;GB0010001^10=231^";
							msg = msg.replace("^", "\u0001");
							single.fromString(msg, new DataDictionary(
									dataDictionaryFile),
									isAvailable);
							
							

						} catch (InvalidMessage e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ConfigError e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Message From file" + single);
						
						send(single, sessionID);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if (recursive && (kind == ENTRY_CREATE)) {
					try {
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
							registerAll(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}

	private void send(quickfix.Message message, SessionID sessionID) {
		try {

			Session.sendToTarget(message, sessionID);
		} catch (SessionNotFound e) {
			System.out.println(e);
		}
	}

	static void usage() {
		System.err.println("usage: java WatchDirectory [-r] dir");
		System.exit(-1);
	}

	public static void main(String[] args) throws IOException, ConfigError {

		System.out.println("Entering FixService ");
		settings = initialize(args);
		Path dir = Paths.get(settings.getString("TapFilePath"));
		FixService fixservice = new FixService(dir, false);
		

		boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true")).booleanValue();

		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
		MessageFactory messageFactory = new DefaultMessageFactory();

		try {
			initiator = new SocketInitiator(fixservice, messageStoreFactory, settings, logFactory, messageFactory);
		} catch (ConfigError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fixservice.logon();
		fixservice.processEvents();
	}

	private static SessionSettings initialize(String[] args) throws FileNotFoundException, ConfigError, IOException {
		InputStream inputStream = null;
		System.out.println("Path "+args[0]);
		if (args.length == 0) {
			inputStream = new BufferedInputStream(new FileInputStream(new File("config/fixservice.cfg")));
		} else if (args.length == 1) {
			inputStream = new FileInputStream(args[0]);
		}
		if (inputStream == null) {
			System.out.println("usage: " + FixService.class.getName() + " [configFile].");
			return null;
		}
		settings = new SessionSettings(inputStream);
		inputStream.close();
		return settings;
	}

	private static class ObservableLogon extends Observable {
		private HashSet<SessionID> set = new HashSet<SessionID>();

		public void logon(SessionID sessionID) {
			set.add(sessionID);
			setChanged();
			notifyObservers(new LogonEvent(sessionID, true));
			clearChanged();
		}

		public void logoff(SessionID sessionID) {
			set.remove(sessionID);
			setChanged();
			notifyObservers(new LogonEvent(sessionID, false));
			clearChanged();
		}
	}
	
	public synchronized void logon() {
        if (!initiatorStarted) {
            try {
                initiator.start();
                initiatorStarted = true;
            } catch (Exception e) {
                System.out.println("Login failed"+e.getLocalizedMessage());
            }
        } else {
            Iterator<SessionID> sessionIds = initiator.getSessions().iterator();
            while (sessionIds.hasNext()) {
                SessionID sessionId = (SessionID) sessionIds.next();
                Session.lookupSession(sessionId).logon();
            }
        }
    }
}
