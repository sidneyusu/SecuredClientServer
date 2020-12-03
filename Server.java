/**
 *
 * @author Sidney Shane Dizon
 * UCID: 10149277
 * Course: CPSC 418 - Assignment 2
 * Description:
 * 	This Server program prompts the user for shared key at startup, to be used as the seed in
 * 	a pseudorandom number generator. The Server Program decrypts the file, check the integrity
 * 	of the file content, and write the file content to the destination file specified by the client.
 * 	This sends an acknowledgement to the client reporting success if the file was correctly decrypted,
 * 	passed the integrity check and written to the destination file; otherwise it will report failure.
 *
 *	The Server echoes any keyboard input obtained by the client.
 *	The program allows a command line argument that defines whether debugging is on or off for the whole program.
 *		-Enabled
 *			-- your program should echo all protocol messages sent and received in such a way that is is clearly
 *				identified which program instance, the call for Server program would be
 *			-- If the 2nd argument in args[] is present and equal to 'debug', then the debug flag is set in the program.
 *
 *
 *	Input as Command-line argument:
 *		i) port - port number for socket connection
 */
import java.io.IOException;
import java.net.*;
import java.util.Vector;


public class Server {
	private ServerSocket serverSock;
	private Vector <ServerThread> serverThreads; //holds the active threads
	private boolean shutdown; //holds the active threads
	private int clientCounter; //id numbers for the clients
	private boolean debugging; //debug flag

	/**
   * Main method
   * @param args First argument should be the port to listen on.
   */
	public static void main(String[] args) {
		if(args.length < 1){
			System.out.println ("Usage: java Server port#");
	    return;
		}
		boolean checkdebug = false;
		if(args.length == 2){
			if(args[1].compareTo("debug") == 0){
				checkdebug = true;
			}
		}
		try{
			Server s = new Server (Integer.parseInt(args[0]), checkdebug);
		} catch (ArrayIndexOutOfBoundsException e){
			System.out.println ("Usage: java Server port#");
	    System.out.println ("First argument is not a port number.");
	    return;
		} catch (NumberFormatException e){
			System.out.println ("Usage: java Server port#");
	    System.out.println ("First argument is not a port number.");
	    return;
		}
	}




	 /**
   * Constructor, makes a new server listening on specified port.
   * @param port The port to listen on.
	 * @param bool The debug on/off flag
   */
	 public Server (int port, boolean bool){
		 clientCounter = 0;
		 shutdown = false;
		 debugging = bool;
		 if(debugging){
			 System.out.println("Debuggin is on for the Server Program.");
		 }
		 try{
 	 		 serverSock = new ServerSocket (port);
		 } catch (IOException e){
			 System.out.println ("Could not create server socket.");
			 return;
		 }
		 /* Server socket open, make a vector to store active threads. */
		 serverThreads = new Vector <ServerThread> (0,1);

		 /* Output connection info for the server */
		 System.out.println("Server IP Address: " + serverSock.getInetAddress().getHostAddress() + ", Port: " + port);
		 /* listen on the socket for connections. */
		 listen ();
	 }



	 /**
   * Allows threads to check and see if the server is shutting down.
   * @return True if the server has been told to shutdown.
   */
	 public boolean getFlag(){
		 return shutdown;
	 }
	 /**
		* Allows threads to check and see if debugging is on.
		* @return True if the debugging is on;
		*/
	 public boolean getDebug(){
		 return debugging;
	 }





	 /**
	 * Called by a thread who's client has asked to exit.  Gets rid of the thread.
	 * @param st The ServerThread to remove from the vector of active connections.
	 */
	 public void kill (ServerThread st){
		 System.out.println("Killing Client: " +st.getID() + ".");
		 /* Find the thread in the vector and remove it. */
		 for(int i=0; i < serverThreads.size(); i++){
			 if (serverThreads.elementAt(i) == st){
				 serverThreads.remove(i);
			 }
		 }
	 }




	 /**
   * Called by a thread who's client has instructed the server to shutdown.
   */
	 public void killall(){
		 shutdown = true;
		 System.out.println ("Shutting Down Server.");
		 /* For each active thread, close it's socket.  This will cause the thread
		 * to stop blocking because of the IO operation, and check the shutdown flag.
		 * The thread will then exit itself when it sees shutdown is true.  Then exits. */
		 for(int i = serverThreads.size() - 1; i >= 0; i--){
			 try{
				 System.out.println ("Killing Client: " + serverThreads.elementAt(i).getID() + ".");
				 serverThreads.elementAt(i).getSocket().close();
			 } catch (IOException e){
				 System.out.println("Could not close socket.");
			 }
			 serverThreads.remove(i);
		 }
		 try{
			 serverSock.close();
		 } catch (IOException e){
			 System.out.println ("Could not close server socket.");
		 }
	 }





	 /**
   * Waits for incoming connections and spins off threads to deal with them.
   */
	 private void listen(){
		 Socket client = new Socket();
		 ServerThread st;

		 /* Should only do this when it hasn't been told to shutdown. */
		 while(!shutdown){
			 /* Try to accept an incoming connection. */
			 try{
				 client = serverSock.accept();
				 /* Output info about the client */
				 System.out.println ("Client on machine " + client.getInetAddress().getHostAddress() + " has connected on port " + client.getLocalPort() + ".");
				 /* Create a new thread to deal with the client, add it to the vector of open connections.
				* Finally, start the thread's execution. Start method makes the threads go by calling their
				* run() methods. */
				st = new ServerThread (client, this, clientCounter++);
				serverThreads.add(st);
				st.start();
			} catch (IOException e){
				/* Server Socket is closed, probably because a client told the server to shutdown */
				System.out.println("Server Socket is closed, probably because a client told the server to shutdown.");
			}
		 }
	 }

}
