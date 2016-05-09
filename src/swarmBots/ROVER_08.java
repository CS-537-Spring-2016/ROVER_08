package swarmBots;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Coord;
import common.Group;
import common.MapTile;
import common.ScanMap;
import enums.Science;
import enums.Terrain;

/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 */

public class ROVER_08 {

	BufferedReader in;
	PrintWriter out;
	String rovername;
	ScanMap scanMap;
	int sleepTime;
	String SERVER_ADDRESS = "localhost";
	static final int PORT_ADDRESS = 9537;

    // all the sockets of blue team - output
    List<Socket> outputSockets = new ArrayList<Socket>();

    // objects contains each rover IP, port, and name
    List<Group> blue = new ArrayList<Group>();

    // every science detected will be added in to this set
    Set<Coord> science_discovered = new HashSet<Coord>();

    // this set contains all the science the ROVERED has shared
    // thus whatever thats in science_collection that is not in display_science
    // are "new" and "unshared"
    Set<Coord> displayed_science = new HashSet<Coord>();

    // ROVER current location
    Coord roverLoc;

    // Your ROVER is going to listen for connection with this
    ServerSocket listenSocket;

	public ROVER_08() {
		// constructor
		System.out.println("ROVER_08 rover object constructed");
		rovername = "ROVER_08";
		SERVER_ADDRESS = "localhost";
		// this should be a safe but slow timer value
		sleepTime = 308; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}
	
	public ROVER_08(String serverAddress) {
		// constructor
		System.out.println("ROVER_08 rover object constructed");
		rovername = "ROVER_08";
		SERVER_ADDRESS = serverAddress;
		sleepTime = 208; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}
	
	
	/**
     * Try to connect each socket on a separate thread. Will try until it works.
     * When socket is created, save it to a LIST
     * 
     * @author Shay
     *
     */
    class RoverComm implements Runnable {

        String ip;
        int port;
        Socket socket;

        public RoverComm(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            do {
                try {
                    socket = new Socket(ip, port);
                } catch (UnknownHostException e) {

                } catch (IOException e) {

                }
            } while (socket == null);
            
            outputSockets.add(socket);
            System.out.println(socket.getPort() + " " + socket.getInetAddress());
        }

    }
    
    /**
     * add all the group's rover into a LIST
     */
    public void initConnection() {
        // dummy value # 1
        blue.add(new Group("Dummy Group #1", "localhost", 53799));

        // blue rooster
        blue.add(new Group("GROUP_01", "localhost", 53701));
        blue.add(new Group("GROUP_02", "localhost", 53702));
        blue.add(new Group("GROUP_03", "localhost", 53703));
        blue.add(new Group("GROUP_04", "localhost", 53704));
        blue.add(new Group("GROUP_05", "localhost", 53705));
        blue.add(new Group("GROUP_06", "localhost", 53706));
        blue.add(new Group("GROUP_07", "localhost", 53707));
        blue.add(new Group("GROUP_09", "localhost", 53709));
    }
    
    /**
     * Create and start a thread for each ROVER connected to you.
     * 
     * @throws IOException
     * @author Shay
     */
    private void startServer() throws IOException {

        // create a thread that waits for client to connect to 
        new Thread(() -> {
            while (true) {
                try {
                    // wait for a connection
                    Socket connectionSocket = listenSocket.accept();

                    // once there is a connection, serve them on thread
                    new Thread(new RoverHandler(connectionSocket)).start();
                    

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * When any ROVER discovered science, it will write a message to your all ROVERS.
     * That message will be "sent" here. This block of code will read whatever
     * written to you. Your job is to use the data to tell your rover to go pick
     * up that science.
     * 
     * @author Shay
     *
     */
    class RoverHandler implements Runnable {
        Socket roverSocket;

        public RoverHandler(Socket socket) {
            this.roverSocket = socket;
        }

        @Override
        public void run() {

            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(roverSocket.getInputStream()));

                while (true) {

                    String line = input.readLine();
                    // protocol: ROCK CRYSTAL 25 30
                    System.out.println("NEW MESSAGE: " + line);
        
                    /*
                     * IMPLEMENT YOUR CODE HERE
                     * WHAT DO YOU WANT TO DO WITH THE DATA?
                     */
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
    
    
	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException, InterruptedException {

		// Make connection to SwarmServer and initialize streams
		Socket socket = null;
		try {
			socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS);

			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			
	         /*
             * This is the server that you are listening on. 
             * This means when we want to contact you, we use that port number.
             */
            listenSocket = new ServerSocket(53701);
            
            /*
             * When you start the server, it will set there and waits for people to connect
             * Once they are connected, a thread is created for each ROVER
             * All  that thread will do is sit there and read all incoming message to you.
             * It is your job to parse the information and use it to gather stuff.
             */
            startServer();
            
            
            /*
             * connect to all the ROVERS on a separate thread
             */
            initConnection();
            for (Group group : blue) {
                new Thread(new RoverComm(group.ip, group.port)).start();
            }
            
            
			// Process all messages from server, wait until server requests Rover ID
			// name - Return Rover Name to complete connection
			while (true) {
				String line = in.readLine();
				if (line.startsWith("SUBMITNAME")) {
					out.println(rovername); // This sets the name of this instance
											// of a swarmBot for identifying the
											// thread to the server
					break;
				}
			}
	
			
			// ********* Rover logic setup *********
			
			String line = "";
			Coord rovergroupStartPosition = null;
			Coord targetLocation = null;
			
			/**
			 *  Get initial values that won't change
			 */
			// **** get equipment listing ****			
			ArrayList<String> equipment = new ArrayList<String>();
			equipment = getEquipment();
			System.out.println(rovername + " equipment list results " + equipment + "\n");
			
			
			// **** Request START_LOC Location from SwarmServer ****
			out.println("START_LOC");
			line = in.readLine();
            if (line == null) {
            	System.out.println(rovername + " check connection to server");
            	line = "";
            }
			if (line.startsWith("START_LOC")) {
				rovergroupStartPosition = extractLocationFromString(line);
			}
			System.out.println(rovername + " START_LOC " + rovergroupStartPosition);
			
			
			// **** Request TARGET_LOC Location from SwarmServer ****
			out.println("TARGET_LOC");
			line = in.readLine();
            if (line == null) {
            	System.out.println(rovername + " check connection to server");
            	line = "";
            }
			if (line.startsWith("TARGET_LOC")) {
				targetLocation = extractLocationFromString(line);
			}
			System.out.println(rovername + " TARGET_LOC " + targetLocation);
			
			

			
	
	
			boolean goingSouth = false;
			boolean goingEast = false;
			boolean goingWest = false;
			boolean goingNorth = false;
			
			boolean stuck = false; // just means it did not change locations between requests,
									// could be velocity limit or obstruction etc.
			boolean blocked = false;
	
			String[] cardinals = new String[4];
			cardinals[0] = "N";
			cardinals[1] = "E";
			cardinals[2] = "S";
			cardinals[3] = "W";
	
			String currentDir = cardinals[0];
			Coord currentLoc = null;
			Coord previousLoc = null;
	

			/**
			 *  ####  Rover controller process loop  ####
			 */
			while (true) {
	
				
				// **** Request Rover Location from SwarmServer ****
				out.println("LOC");
				line = in.readLine();
	            if (line == null) {
	            	System.out.println(rovername + " check connection to server");
	            	line = "";
	            }
				if (line.startsWith("LOC")) {
					// loc = line.substring(4);
					currentLoc = extractLocationFromString(line);
					
					// class variable
                    roverLoc = extractLocationFromString(line);
					
				}
				System.out.println(rovername + " currentLoc at start: " + currentLoc);
				
				// after getting location set previous equal current to be able to check for stuckness and blocked later
				previousLoc = currentLoc;		
				
				

				
		
	
				// ***** do a SCAN *****

				// gets the scanMap from the server based on the Rover current location
				doScan(); 
				// prints the scanMap to the Console output for debug purposes
				scanMap.debugPrintMap();
				
				
                // ****************** Check scan map for science and shared them *****

                detectCrystal(scanMap.getScanMap());
                System.out.println("SCIENCE DISCOVERED: " + science_discovered);
                shareScience();

                // *********************************************************************
				
				
				// ***** get TIMER remaining *****
				out.println("TIMER");
				line = in.readLine();
	            if (line == null) {
	            	System.out.println(rovername + " check connection to server");
	            	line = "";
	            }
				if (line.startsWith("TIMER")) {
					String timeRemaining = line.substring(6);
					System.out.println(rovername + " timeRemaining: " + timeRemaining);
				}
				
				
	
				
				// ***** MOVING *****
				// try moving east 5 block if blocked
				if (blocked) {
					for (int i = 0; i < 5; i++) {
						out.println("MOVE E");
						//System.out.println("ROVER_08 request move E");
						Thread.sleep(308);
					}
					blocked = false;
					//reverses direction after being blocked
					goingSouth = !goingSouth;
				} else {
	
					// pull the MapTile array out of the ScanMap object
					MapTile[][] scanMapTiles = scanMap.getScanMap();
					int centerIndex = (scanMap.getEdgeSize() - 1)/2;
					// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
	
					if (goingSouth) {
						// check scanMap to see if path is blocked to the south
						// (scanMap may be old data by now)

						
						if (!scanMapTiles[centerIndex][centerIndex].getScience().getSciString().equals("N")) {
							System.out.println("ROVER_08 request GATHER");
							out.println("GATHER");
							
						}
						
						if (scanMapTiles[centerIndex][centerIndex +1].getHasRover() 
								|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.ROCK
								|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.NONE) {
							blocked = true;
						} else {
							// request to server to move
							out.println("MOVE S");
							//System.out.println("ROVER_08 request move S");
						}
						
					}else if(goingEast)
					{
						
						
						if (!scanMapTiles[centerIndex][centerIndex].getScience().getSciString().equals("N")) {
							System.out.println("ROVER_08 request GATHER");
							out.println("GATHER");
							
						}
						
						if (scanMapTiles[centerIndex][centerIndex +1].getHasRover() 
								|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.ROCK
								|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.NONE) {
							blocked = true;
						} else {
							// request to server to move
							out.println("MOVE E");
							//System.out.println("ROVER_08 request move E");
						}
						
						
					}else {
						// check scanMap to see if path is blocked to the north
						// (scanMap may be old data by now)
						//System.out.println("ROVER_08 scanMapTiles[2][1].getHasRover() " + scanMapTiles[2][1].getHasRover());
						//System.out.println("ROVER_08 scanMapTiles[2][1].getTerrain() " + scanMapTiles[2][1].getTerrain().toString());
						
						if (!scanMapTiles[centerIndex][centerIndex].getScience().getSciString().equals("N")) {
							System.out.println("ROVER_08 request GATHER");
							out.println("GATHER");
							
						}
						
						if (scanMapTiles[centerIndex][centerIndex -1].getHasRover() 
								|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.ROCK
								|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.NONE) {
							blocked = true;
						} else {
							// request to server to move
							out.println("MOVE N");
							//System.out.println("ROVER_08 request move N");
						}					
					}
				}
	
				// another call for current location
				out.println("LOC");
				line = in.readLine();
				if(line == null){
					System.out.println("ROVER_08 check connection to server");
					line = "";
				}
				if (line.startsWith("LOC")) {
					currentLoc = extractLocationFromString(line);
					
				}
	
	
				// test for stuckness
				stuck = currentLoc.equals(previousLoc);
	
				//System.out.println("ROVER_08 stuck test " + stuck);
				System.out.println("ROVER_08 blocked test " + blocked);
	
				// TODO - logic to calculate where to move next
	
				
				// this is the Rovers HeartBeat, it regulates how fast the Rover cycles through the control loop
				Thread.sleep(sleepTime);
				
				System.out.println("ROVER_08 ------------ bottom process control --------------"); 
			}
		
		// This catch block closes the open socket connection to the server
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
	        if (socket != null) {
	            try {
	            	socket.close();
	            } catch (IOException e) {
	            	System.out.println("ROVER_08 problem closing socket");
	            }
	        }
	    }

	} // END of Rover main control loop
	
	// ####################### Support Methods #############################
	
	
	/**
     * iterate through a scan map to find a tile with radiation. get the
     * adjusted (absolute) coordinate of the tile and added into a hash set
     * 
     * @param scanMapTiles
     * @author Shay
     */
    private void detectCrystal(MapTile[][] scanMapTiles) {
        for (int x = 0; x < scanMapTiles.length; x++) {
            for (int y = 0; y < scanMapTiles[x].length; y++) {
                MapTile mapTile = scanMapTiles[x][y];
                if (mapTile.getScience() == Science.CRYSTAL) {
                    int tileX = roverLoc.xpos + (x - 3);
                    int tileY = roverLoc.ypos + (y - 3);
                    Coord coord = new Coord(mapTile.getTerrain(), mapTile.getScience(), tileX, tileY);
                    science_discovered.add(coord);
                }
            }
        }
    }
    
    
    /**
     * write to each rover the coords of a tile that contains radiation. will
     * only write to them if the coords are new.
     * 
     * @author Shay
     */
    private void shareScience() {
        for (Coord c : science_discovered) {
            if (!displayed_science.contains(c)) {
                for (Socket s : outputSockets)
                    try {
                        new DataOutputStream(s.getOutputStream()).writeBytes(c.toString() + "\r\n");
                    } catch (Exception e) {

                    }
                displayed_science.add(c);
            }
        }
    }
    
    
	private void clearReadLineBuffer() throws IOException{
		while(in.ready()){
			//System.out.println("ROVER_08 clearing readLine()");
			in.readLine();	
		}
	}
	

	// method to retrieve a list of the rover's EQUIPMENT from the server
	private ArrayList<String> getEquipment() throws IOException {
		//System.out.println("ROVER_08 method getEquipment()");
		Gson gson = new GsonBuilder()
    			.setPrettyPrinting()
    			.enableComplexMapKeySerialization()
    			.create();
		out.println("EQUIPMENT");
		
		String jsonEqListIn = in.readLine(); //grabs the string that was returned first
		if(jsonEqListIn == null){
			jsonEqListIn = "";
		}
		StringBuilder jsonEqList = new StringBuilder();
		//System.out.println("ROVER_08 incomming EQUIPMENT result - first readline: " + jsonEqListIn);
		
		if(jsonEqListIn.startsWith("EQUIPMENT")){
			while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
				if(jsonEqListIn == null){
					break;
				}
				//System.out.println("ROVER_08 incomming EQUIPMENT result: " + jsonEqListIn);
				jsonEqList.append(jsonEqListIn);
				jsonEqList.append("\n");
				//System.out.println("ROVER_08 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return null; // server response did not start with "EQUIPMENT"
		}
		
		String jsonEqListString = jsonEqList.toString();		
		ArrayList<String> returnList;		
		returnList = gson.fromJson(jsonEqListString, new TypeToken<ArrayList<String>>(){}.getType());		
		//System.out.println("ROVER_08 returnList " + returnList);
		
		return returnList;
	}
	

	// sends a SCAN request to the server and puts the result in the scanMap array
	public void doScan() throws IOException {
		//System.out.println("ROVER_08 method doScan()");
		Gson gson = new GsonBuilder()
    			.setPrettyPrinting()
    			.enableComplexMapKeySerialization()
    			.create();
		out.println("SCAN");

		String jsonScanMapIn = in.readLine(); //grabs the string that was returned first
		if(jsonScanMapIn == null){
			System.out.println("ROVER_08 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();
		System.out.println("ROVER_08 incomming SCAN result - first readline: " + jsonScanMapIn);
		
		if(jsonScanMapIn.startsWith("SCAN")){	
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				//System.out.println("ROVER_08 incomming SCAN result: " + jsonScanMapIn);
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
				//System.out.println("ROVER_08 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}
		//System.out.println("ROVER_08 finished scan while");

		String jsonScanMapString = jsonScanMap.toString();
		// debug print json object to a file
		//new MyWriter( jsonScanMapString, 0);  //gives a strange result - prints the \n instead of newline character in the file

		//System.out.println("ROVER_08 convert from json back to ScanMap class");
		// convert from the json string back to a ScanMap object
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);		
	}
	

	// this takes the server response string, parses out the x and x values and
	// returns a Coord object	
	public static Coord extractLocationFromString(String sStr) {
		int indexOf;
		indexOf = sStr.indexOf(" ");
		sStr = sStr.substring(indexOf +1);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			//System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			//System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}
	

	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
		ROVER_08 client = new ROVER_08();
		client.run();
	}
}
