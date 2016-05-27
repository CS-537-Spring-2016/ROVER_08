package swarmBots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
//import com.sun.org.apache.xpath.internal.operations.Bool;

import common.Coord;
import common.MapTile;
import common.ScanMap;
import communication.Group;
import communication.RoverCommunication;
import enums.RoverDriveType;
import enums.RoverToolType;
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
	int sleepTime=150;
	String SERVER_ADDRESS = "localhost";//"192.168.1.106";
	static final int PORT_ADDRESS = 9537;
	
	
	boolean goingSouth = false,traverseJackpot=Boolean.FALSE;
	boolean goingEast = false;
	boolean goingWest = false;
	boolean goingNorth = false;
	boolean goingHorizontal = false;
	boolean blocked = false;
	boolean blockedByRover = false;
	
	Coord targetLocation = null;
	
	/* Communication Module*/
    RoverCommunication rocom;

	public ROVER_08() {
		// constructor
		System.out.println("ROVER_08 rover object constructed");
		rovername = "ROVER_08";
		SERVER_ADDRESS = "localhost";
		// this should be a safe but slow timer value
		sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}
	
	public ROVER_08(String serverAddress) {
		// constructor
		System.out.println("ROVER_08 rover object constructed");
		rovername = "ROVER_08";
		SERVER_ADDRESS = serverAddress;
		sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
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
			
			
            // ******************* SET UP COMMUNICATION MODULE by Shay *********************
            /* Your Group Info*/
            Group group = new Group(rovername, SERVER_ADDRESS, 53708, RoverDriveType.TREADS,
                    RoverToolType.HARVESTER, RoverToolType.SPECTRAL_SENSOR);

            /* Setup communication, only communicates with gatherers */
            rocom = new RoverCommunication(group);
            rocom.setGroupList(Group.getGatherers());

            /* Can't go on ROCK, thus ignore any SCIENCE COORDS that is on ROCK */
            rocom.ignoreTerrain(Terrain.ROCK);

            /* Start your server, receive incoming message from other ROVERS */
            rocom.startServer();
            // ******************************************************************
	
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
			
			

			boolean stuck = false; // just means it did not change locations between requests,
									// could be velocity limit or obstruction etc.
			blocked = false;
	
			String[] cardinals = new String[4];
			cardinals[0] = "N";
			cardinals[1] = "E";
			cardinals[2] = "S";
			cardinals[3] = "W";
	
			String currentDir = cardinals[0];
			Coord currentLoc = null;
			Coord previousLoc = null;
			List<Coord> crystalList;
			String dir;
			/**
			 *  ####  Rover controller process loop  ####
			 */
			while (true) {
				
				currentLoc = getCurrentLoaction();
					
				// after getting location set previous equal current to be able to check for stuckness and blocked later
				previousLoc = currentLoc;		
							
				// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
				MapTile[][] scanMapTiles =getScanMapTiles();
				int centerIndex = (scanMap.getEdgeSize() - 1)/2;
				
				// ***** MOVING *****
				// try moving east 5 block if blocked
				if(blockedByRover)
				{
					dir=generateRandomDirection();
					setDirection(dir);
					moveRover(scanMapTiles, centerIndex);
					
					blocked = Boolean.FALSE;
					blockedByRover = Boolean.FALSE;
					Thread.sleep(sleepTime);
					
					/*moveWhenBlocked(scanMapTiles, centerIndex);
					blocked=Boolean.FALSE;
					blockedByRover=Boolean.FALSE;
					Thread.sleep(sleepTime);*/
					
				}
				else if (blocked) {
					
						moveWhenBlocked(scanMapTiles, centerIndex);
						Thread.sleep(sleepTime);
						
					for (int i = 0; i < 6 ; i++) {
						
						scanMapTiles=getScanMapTiles();
						dir=generateRandomDirection();
						setDirection(dir);
						moveRover(scanMapTiles, centerIndex);
						blocked = Boolean.FALSE;
						blockedByRover = Boolean.FALSE;
						Thread.sleep(sleepTime);
						currentLoc=getCurrentLoaction();
						scanMapTiles =getScanMapTiles();
						
						}
					}else {
						
				 crystalList=getCrystalLocation(scanMapTiles,currentLoc,new ArrayList<Coord>() );
					if(crystalList.size()>0)
					{
						gatherCrystal( crystalList, currentLoc, scanMapTiles);
				
					}
					
					if(blocked==Boolean.FALSE && blockedByRover==Boolean.FALSE){
						getTargetDirection(currentLoc, targetLocation);
						moveRover(scanMapTiles,centerIndex);
					}
					currentLoc=getCurrentLoaction();
					scanMapTiles=getScanMapTiles();
					
					if(currentLoc.xpos==targetLocation.xpos && currentLoc.ypos==targetLocation.ypos)
					{
						if(!traverseJackpot)
						{
							gatherInJackpot(scanMapTiles,centerIndex);
							traverseJackpot=Boolean.TRUE;
						}
						crystalList=null;
						crystalList=rocom.getAllGatherableDiscoveredSciences();
						if(crystalList!=null && crystalList.size()>0)
						{
							gatherSharedCrystal( crystalList, currentLoc, scanMapTiles);
						}
						
					}
					
					}
	
			
	
	
				// test for stuckness
				stuck = currentLoc.equals(previousLoc);
	
				//System.out.println("ROVER_08 stuck test " + stuck);
				System.out.println("ROVER_08 blocked test " + blocked);
	
				// TODO - logic to calculate where to move next
				
				
                /* ********* Detect and Share Science ***************/
                rocom.detectAndShare(scanMapTiles, currentLoc, 3);
                /* *************************************************/
				
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
	
	public void gatherInJackpot(MapTile[][] scanMapTiles, int centerIndex) throws Exception {
		
		int i,j,xPos,yPos;
		Boolean flag;
		String dir;
//		targetLocation=targetLocation;
		Coord currentLocation = getCurrentLoaction();
		List<Coord> list= new ArrayList<Coord>();
		for(i=0;i<7;i++){
			for(j=0;j<7;j++)
			{
				if((scanMapTiles[i][i].getTerrain() == Terrain.SOIL || scanMapTiles[i][j].getTerrain() == Terrain.SAND || scanMapTiles[i][j].getTerrain() == Terrain.GRAVEL ))
				{
					if(i<3)
					{
						xPos=(currentLocation.xpos-(4-i)+1);
					}
					else if(i>3)
					{
						xPos=(currentLocation.xpos+(i%4)+1);
					}
					else
					{
						xPos=currentLocation.xpos;
					}
					if(j<3)
					{
						yPos=currentLocation.ypos-(4-j)+1;

					}else if(j>3)
					{
						yPos=currentLocation.ypos+(j%4)+1;
						
					}
					else{
						yPos=currentLocation.ypos;
					}
					
					
						list.add(new Coord(xPos,yPos) );	
					
				}
			
			}
		}
		
		for (Coord c : list) {
			
			flag=Boolean.TRUE;
			while(flag){
			
				
				scanMapTiles=getScanMapTiles();
				getTargetDirection(currentLocation, c);
				moveRover(scanMapTiles,centerIndex );
				currentLocation = getCurrentLoaction();
				if(currentLocation.xpos==c.xpos && currentLocation.ypos==c.ypos)
				{
					flag=Boolean.FALSE;
				}
				Thread.sleep(sleepTime);
				
				if(blocked || blockedByRover )
				{
				/*	scanMapTiles=getScanMapTiles();
					moveWhenBlocked(scanMapTiles, centerIndex);
					break;*/
					
						for ( i = 0; i < 4 ; i++) {
							currentLocation=getCurrentLoaction();
						scanMapTiles=getScanMapTiles();
						dir=generateRandomDirection();
						setDirection(dir);
						moveRover(scanMapTiles, centerIndex);
						blocked = Boolean.FALSE;
						blockedByRover = Boolean.FALSE;
						Thread.sleep(sleepTime);
						
						
						
						}
				}
			
			}
		}
	}

	// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
	private void moveWhenBlocked(MapTile[][]scanMapTiles, int x) throws Exception {
		Boolean north,east,south,west;
		north=Boolean.FALSE;
		east=Boolean.FALSE;
		south=Boolean.FALSE;
		west=Boolean.FALSE;
		
		if(checkNorthDirection(scanMapTiles, x, x))
			north=Boolean.TRUE;
		if(checkEastDirection(scanMapTiles, x, x))
			east=Boolean.TRUE;
		if(checkSouthDirection(scanMapTiles, x, x))
			south=Boolean.TRUE;
		if(checkWestDirection(scanMapTiles, x, x))
			west=Boolean.TRUE;
		
		if(north)
			setDirection("N");
		else if(east)
			setDirection("E");
			else if(south)
				setDirection("S");
				else if(west)
					setDirection("W");
		
		moveRover(scanMapTiles, x);
			
		
	}

	private MapTile[][] getScanMapTiles() throws Exception {
		// ***** do a SCAN *****
		// gets the scanMap from the server based on the Rover current location
		doScan(); 
		// prints the scanMap to the Console output for debug purposes
		scanMap.debugPrintMap();
		 return scanMap.getScanMap();
		
	}

	// ####################### Support Methods #############################
	
	private Coord getCurrentLoaction() throws Exception {
		String line;
		Coord currentLoc=null;
		out.println("LOC");
		line = in.readLine();
		if(line == null){
			System.out.println("ROVER_08 check connection to server");
			line = "";
		}
		if (line.startsWith("LOC")) {
			currentLoc = extractLocationFromString(line);
			System.out.println(rovername + " currentLoc at start: " + currentLoc);
		}
		
		return currentLoc;
		
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
	
	public Boolean validateMapTile(MapTile map) {
		
		if ( map.getHasRover() == Boolean.TRUE){
			blockedByRover=Boolean.TRUE;
			blocked=Boolean.FALSE;
			return Boolean.FALSE;
		}
		if (map.getTerrain() == Terrain.ROCK || map.getTerrain() == Terrain.NONE){
			blocked=Boolean.TRUE;
			blockedByRover=Boolean.FALSE;
			return Boolean.FALSE;
		}

		return Boolean.TRUE;

	}
	
	public String generateRandomDirection() {

		Random ran = new Random();
	
		int i =  ran.nextInt(1000)%4;
		if(i==0)
		{
			return "S";
			//goingSouth=Boolean.TRUE;goingEast=Boolean.FALSE;goingWest=Boolean.FALSE;goingNorth=Boolean.FALSE;
			
		}
		else if(i ==1)
		{
			return "N";
//			goingSouth=Boolean.FALSE;goingEast=Boolean.FALSE;goingWest=Boolean.FALSE;goingNorth=Boolean.TRUE;
		}
		else if(i==2)
		{
			return "W";
//			goingSouth=Boolean.FALSE;goingEast=Boolean.FALSE;goingWest=Boolean.TRUE;goingNorth=Boolean.FALSE;
		}else
		{
			return "E";
//			goingSouth=Boolean.FALSE;goingEast=Boolean.TRUE;goingWest=Boolean.FALSE;goingNorth=Boolean.FALSE;
		}

	}
	
	public void setDirection(String dir)
	{
		
		if(dir.equals("S")){
			goingSouth=Boolean.TRUE;goingEast=Boolean.FALSE;goingWest=Boolean.FALSE;goingNorth=Boolean.FALSE;
		}else if(dir.equals("N")){
			goingSouth=Boolean.FALSE;goingEast=Boolean.FALSE;goingWest=Boolean.FALSE;goingNorth=Boolean.TRUE;
		}else if(dir.equals("W")){
			goingSouth=Boolean.FALSE;goingEast=Boolean.FALSE;goingWest=Boolean.TRUE;goingNorth=Boolean.FALSE;
		}else if(dir.equals("E")){
			goingSouth=Boolean.FALSE;goingEast=Boolean.TRUE;goingWest=Boolean.FALSE;goingNorth=Boolean.FALSE;
		}else
			generateRandomDirection();
		
	}
	
	public void getTargetDirection(Coord current,Coord target) throws Exception {

		
		MapTile[][] map = scanMap.getScanMap();
		int x = (scanMap.getEdgeSize() - 1) / 2;
		// S = y + 1; N = y - 1; E = x + 1; W = x - 1
		if (current.xpos == target.xpos
				&& current.ypos == target.ypos) {
//			directionChecker();
		} else if ((current.xpos < target.xpos && current.ypos < target.ypos)) {
			if (goingHorizontal) {
					goingSouth = Boolean.TRUE;
					goingNorth = Boolean.FALSE;
					goingEast = Boolean.FALSE;
					goingWest = Boolean.FALSE;
				} else {
					goingSouth = Boolean.FALSE;
					goingNorth = Boolean.FALSE;
					goingEast = Boolean.TRUE;
					goingWest = Boolean.FALSE;
				
			}

		} else if (current.xpos == target.xpos) {

			if (current.ypos < target.ypos) {
					goingSouth = Boolean.TRUE;
					goingNorth = Boolean.FALSE;
					goingEast = Boolean.FALSE;
					goingWest = Boolean.FALSE;
				
			} else {
					goingSouth = Boolean.FALSE;
					goingNorth = Boolean.TRUE;
					goingEast = Boolean.FALSE;
					goingWest = Boolean.FALSE;
			}
			} else if (current.ypos == target.ypos) {
				if (current.xpos < target.xpos) {
						goingSouth = Boolean.FALSE;
						goingNorth = Boolean.FALSE;
						goingEast = Boolean.TRUE;
						goingWest = Boolean.FALSE;
				} else {
						goingSouth = Boolean.FALSE;
						goingNorth = Boolean.FALSE;
						goingEast = Boolean.FALSE;
						goingWest = Boolean.TRUE;
					
				}

		} else if (current.xpos > target.xpos) {
				goingSouth = Boolean.FALSE;
				goingNorth = Boolean.FALSE;
				goingEast = Boolean.FALSE;
				goingWest = Boolean.TRUE;
			
		} else {
				goingSouth = Boolean.FALSE;
				goingNorth = Boolean.FALSE;
				goingEast = Boolean.TRUE;
				goingWest = Boolean.FALSE;
		}

	}
	
	public void moveRover(MapTile[][] scanMapTiles,int centerIndex) throws Exception
	{
	/*	if (!scanMapTiles[centerIndex][centerIndex].getScience().getSciString().equals("N")) {
			System.out.println("ROVER_08 request GATHER");
			out.println("GATHER");
			Thread.sleep(sleepTime);
			
		}*/
		
		out.println("GATHER"); 
		
		// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
		if (goingSouth) {
			
			goingHorizontal = Boolean.FALSE;
			if (!checkSouthDirection(scanMapTiles, centerIndex, centerIndex)) {
				blocked = true;
			} else {
				out.println("MOVE S");
			}
			
		}else if(goingEast)
		{
			goingHorizontal = Boolean.TRUE;
		
				if (!checkEastDirection(scanMapTiles, centerIndex, centerIndex)) {
					blocked = true;
				} else {
					out.println("MOVE E");
				}
			}else if (goingWest) {
			goingHorizontal = Boolean.TRUE;

			if (!checkWestDirection(scanMapTiles, centerIndex, centerIndex)) {
				blocked = true;
			} else {
				out.println("MOVE W");
			}

		}else {
			goingHorizontal = Boolean.FALSE;
			if (!checkNorthDirection(scanMapTiles,centerIndex,centerIndex)) {
				blocked = true;
			} else {
				out.println("MOVE N");
			}					
		} 
		
		/*if(blockedByRover)
		{
			blocked=Boolean.FALSE;
			blockedByRover=Boolean.FALSE;
			 getCurrentLoaction();
			scanMapTiles =getScanMapTiles();
			moveWhenBlocked(scanMapTiles, centerIndex);
			
		
			
		}
		else if (blocked) {
			blocked=Boolean.FALSE;
			blockedByRover=Boolean.FALSE;
			for (int i = 0; i < 5; i++) {
				
				 getCurrentLoaction();
				scanMapTiles =getScanMapTiles();
				moveWhenBlocked(scanMapTiles, centerIndex);
				Thread.sleep(sleepTime);
				
			}
			
		}*/
		
		Thread.sleep(sleepTime);
		
	} 
	// checks for obstacle in North Direction
	Boolean checkNorthDirection(MapTile[][] map, int x, int y) {
		if (validateMapTile(map[x][y - 1])) // North
		{
			goingSouth = Boolean.FALSE;
			goingNorth = Boolean.TRUE;
			goingEast = Boolean.FALSE;
			goingWest = Boolean.FALSE;
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	// checks for obstacle in East Direction
	Boolean checkEastDirection(MapTile[][] map, int x, int y) {
		if (validateMapTile(map[x + 1][y])) // East
		{
			goingSouth = Boolean.FALSE;
			goingNorth = Boolean.FALSE;
			goingEast = Boolean.TRUE;
			goingWest = Boolean.FALSE;
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	// checks for obstacle in South Direction
	Boolean checkSouthDirection(MapTile[][] map, int x, int y) {
		if (validateMapTile(map[x][y + 1])) // South
		{
			goingSouth = Boolean.TRUE;
			goingNorth = Boolean.FALSE;
			goingEast = Boolean.FALSE;
			goingWest = Boolean.FALSE;
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	// checks for obstacle in West Direction
	Boolean checkWestDirection(MapTile[][] map, int x, int y) {
		if (validateMapTile(map[x - 1][y])) // West
		{
			goingSouth = Boolean.FALSE;
			goingNorth = Boolean.FALSE;
			goingEast = Boolean.FALSE;
			goingWest = Boolean.TRUE;
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
	
	public List<Coord> getCrystalLocation(MapTile[][] scanMapTiles ,Coord currentLoc, List<Coord> list)
	{
		int i,j,s,xPos = 0,yPos=0;
		Boolean isPresent=Boolean.FALSE;
		if(list==null)
		 list= new ArrayList<Coord>();
		
		
		
		for(i=0;i<7;i++)
		{
			for(j=0;j<7;j++)
			{
				isPresent=Boolean.FALSE;
				
				if((scanMapTiles[i][j].getTerrain() == Terrain.SOIL || scanMapTiles[i][j].getTerrain() == Terrain.SAND )){
					if(!scanMapTiles[i][j].getScience().getSciString().equals("N"))	{
					if(i<3)
					{
						xPos=(currentLoc.xpos-(4-i)+1);
					}
					else if(i>3)
					{
						xPos=(currentLoc.xpos+(i%4)+1);
					}
					else
					{
						xPos=currentLoc.xpos;
					}
					if(j<3)
					{
						yPos=currentLoc.ypos-(4-j)+1;

					}else if(j>3)
					{
						yPos=currentLoc.ypos+(j%4)+1;
						
					}
					else{
						yPos=currentLoc.ypos;
					}
					
					for( s=0;s<list.size() && isPresent==Boolean.FALSE;s++)
					{
						if(list.get(s).getXpos()==xPos && list.get(s).getYpos()==yPos)
						{
							isPresent=Boolean.TRUE;
						}
					}
					
					if(!isPresent)
						list.add(new Coord(xPos,yPos) );	
					
					
				}
			}	
					
			}
		}
		return list;
		
	}
	
	
	void gatherSharedCrystal(List<Coord> crystalList,Coord currentLocation,MapTile[][] scanMapTiles) throws Exception
	{
		int i , centerIndex = (scanMap.getEdgeSize() - 1)/2;
		Boolean flag=Boolean.TRUE;
		String dir;
		for( i=0;i<crystalList.size();i++)
		{
			flag=Boolean.TRUE;
			while(flag){
			
				getTargetDirection(currentLocation, crystalList.get(i));
				moveRover(scanMapTiles,centerIndex );
				if(currentLocation.xpos==crystalList.get(i).xpos && currentLocation.ypos==crystalList.get(i).ypos)
				{
					flag=Boolean.FALSE;
				}
				currentLocation = getCurrentLoaction();
				scanMapTiles=getScanMapTiles();
				rocom.detectAndShare(scanMapTiles, currentLocation, 3);
	            
				Thread.sleep(sleepTime);
			
				if(blockedByRover)
				{
					dir=generateRandomDirection();
					setDirection(dir);
					moveRover(scanMapTiles, centerIndex);
					currentLocation = getCurrentLoaction();
					
					blocked = Boolean.FALSE;
					blockedByRover = Boolean.FALSE;
					Thread.sleep(sleepTime);
					
				}
				else if (blocked) {
					
						moveWhenBlocked(scanMapTiles, centerIndex);
						blocked = Boolean.FALSE;
						blockedByRover = Boolean.FALSE;
						Thread.sleep(sleepTime);
						
					for (int j = 0; j < 6 ; j++) {
						
						scanMapTiles=getScanMapTiles();
						dir=generateRandomDirection();
						setDirection(dir);
						moveRover(scanMapTiles, centerIndex);
						
						Thread.sleep(sleepTime);
						
						}
					currentLocation=getCurrentLoaction();
					scanMapTiles =getScanMapTiles();
					}
				
			}
		}
		
	}
	
	void gatherCrystal(List<Coord> crystalList,Coord currentLocation,MapTile[][] scanMapTiles) throws Exception
	{
		int i , centerIndex = (scanMap.getEdgeSize() - 1)/2;
		Boolean flag=Boolean.TRUE;
		for( i=0;i<crystalList.size();i++)
		{
			flag=Boolean.TRUE;
			while(flag==true && blocked==false &&  blockedByRover==false){
			
				getTargetDirection(currentLocation, crystalList.get(i));
				moveRover(scanMapTiles,centerIndex );
				if(currentLocation.xpos==crystalList.get(i).xpos && currentLocation.ypos==crystalList.get(i).ypos)
				{
					flag=Boolean.FALSE;
				}
				currentLocation = getCurrentLoaction();
				scanMapTiles=getScanMapTiles();
				getCrystalLocation(scanMapTiles,currentLocation,crystalList);
				
				rocom.detectAndShare(scanMapTiles, currentLocation, 3);
	            
				Thread.sleep(sleepTime);
			
			}
		}
	/*		for( i=0;i<crystalList.size();i++)
		{
			flag=Boolean.TRUE;
			while(flag==true && blocked==false &&  blockedByRover==false){
				
			getTargetDirection(currentLoc, crystalList.get(i));
			moveRover(scanMapTiles,centerIndex );
		   	
			if(currentLoc.xpos==crystalList.get(i).xpos && currentLoc.ypos==crystalList.get(i).ypos)
			{
				flag=Boolean.FALSE;
			}
			currentLoc = getCurrentLoaction();
			scanMapTiles=getScanMapTiles();
			getCrystalLocation(scanMapTiles,currentLoc,crystalList);
			
			   ********* Detect and Share Science **************
            rocom.detectAndShare(scanMapTiles, currentLoc, 3);
             ************************************************
			
			
			Thread.sleep(sleepTime);
			}
		}	*/	
	}

	/*public void moveWhenBlocked(MapTile[][] scanMapTiles,int centerIndex) throws Exception
	{
		String dir;
		dir=generateRandomDirection();
		setDirection(dir);
		moveRover(scanMapTiles, centerIndex);
		
		blocked = Boolean.FALSE;
		blockedByRover = Boolean.FALSE;
		Thread.sleep(sleepTime);
	}*/

	
	
	
	
	
	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
//		ROVER_08 client = new ROVER_08("192.168.1.106");
		ROVER_08 client = new ROVER_08();
		client.run();
	}
}
