package testUtillities;

import common.Rover;
import enums.RoverName;
import org.junit.*;

public class RoverNameTest {
	public static void main(String[] args) {
		System.out.println("test roverName test running");
		
		String name = "ROVER_08";
        RoverName rname = RoverName.getEnum(name); 
        System.out.println("SWARM: make a rover name " + rname);
        new RoverNameTest().RoverNameTestMethod();
        
        
	}
	
	@Test
	public void RoverNameTestMethod()
	{
			String name = "ROVER_08";
		  assert name.equals(RoverName.getEnum(name).toString());
	}
}
