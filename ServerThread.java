/**
 *
 * @author Sidney Shane Dizon
 * UCID: 10149277
 * Course: CPSC 418 - Assignment 2
 *
 * Description:
 * 	This ServerThread Program contains a class for the thread to deal with clients who connect
 * 	to the Server Program.
 *
 */
import java.net.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ServerThread extends Thread{
	private Socket sock;  //The socket it communicates with the client on.
	private Server parent;  //Reference to Server object for message passing.
	private int idnum;  //The client's id number.
	private static KeyGenerator keyGen = null;
	private static KeyGenerator hmacKey = null;
	private static SecretKey secKey = null;
	private static byte[] raw = null;
	private static SecretKeySpec secKeySpec = null;
	private static Cipher secCipher = null;
	private static int digestLength = 20;

	/**
	 * Constructor, does the usual stuff.
	 * @param s Communication Socket.
	 * @param p Reference to parent thread.
	 * @param id ID Number.
	 */
	public ServerThread (Socket s, Server p, int id){
		parent = p;
		sock = s;
		idnum = id;
	}
	/**
	* Getter for id number.
	* @return ID Number
	*/
	public int getID (){
		return idnum;
	}
    /**
    * Getter for the socket, this way the parent thread can
    * access the socket and close it, causing the thread to
    * stop blocking on IO operations and see that the server's
    * shutdown flag is true and terminate.
    * @return The Socket.
    */
    public Socket getSocket (){
    	return sock;
    }
    /**
    * This is what the thread does as it executes.  Listens on the socket
    * for incoming data and then echos it to the screen.  A client can also
    * ask to be disconnected with "exit" or to shutdown the server with "die".
    */
   public void run (){
		DataInputStream input = null;
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String sharedKey;
		DataOutputStream writer = null;
		try {
			input = new DataInputStream(sock.getInputStream());
			writer = new DataOutputStream(sock.getOutputStream());
		} catch (UnknownHostException e) {
			System.out.println ("Unknown host error.");
			return;
		} catch (IOException e) {
			System.out.println ("Could not establish communication.");
			return;
		}

		/* Try to read from the socket */
		/* Prompt the user to enter the shared key */
		try{
			System.out.println("Please type in the shared key with Client " + idnum + " :");
			sharedKey = stdIn.readLine();
		if(parent.getDebug()){
			System.out.println("shared Key: " + sharedKey);
		}
   
		/* Do the PNG so that we can generate 128-bit session key */
		//SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(sharedKey.getBytes());
		if(parent.getDebug()){
			byte[] result = new byte[128/8];
			random.nextBytes(result);
			System.out.println("SecureRandom: " + result);
		}
		//Generate the keys
		hmacKey = KeyGenerator.getInstance("HMACSHA1");
		hmacKey.init(128, random);
		SecretKey secretKey = hmacKey.generateKey();
	    if(parent.getDebug()){
			byte[] result = secretKey.getEncoded();
	        System.out.println("Hmac Key Generated: " + result);
	      }
	    Mac mac = Mac.getInstance("HMACSHA1");
		mac.init(secretKey);
		//Generate 128 AES Key
		keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128, random);
		secKey = keyGen.generateKey();
		raw = secKey.getEncoded();
		secKeySpec = new SecretKeySpec(raw, "AES");
		secCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	
		//Generate IV 
		byte[] iv = new byte[128/8];
		random.nextBytes(iv);
		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		
		//Get the messages from the Client Program
		byte[] dfnEncrypted = new byte[input.readInt()];
		input.readFully(dfnEncrypted);
		byte[] lenEncrypted = new byte[input.readInt()];
		input.readFully(lenEncrypted);
		byte[] conEncrypted = new byte[input.readInt()];
		input.readFully(conEncrypted);
	
	
		if(parent.getDebug()){
			System.out.println ("Client " + idnum + ": " + dfnEncrypted + " : " + dfnEncrypted.length);
			System.out.println ("Client " + idnum + ": " + lenEncrypted + " : " + lenEncrypted.length);
			System.out.println ("Client " + idnum + ": " + conEncrypted + " : " + conEncrypted.length);
		}
	
		/* Decrypt the incoming files */
		secCipher.init(Cipher.DECRYPT_MODE, secKeySpec, ivSpec);
		byte[] decryptedDFN = secCipher.doFinal(dfnEncrypted);
		byte[] decryptedLen = secCipher.doFinal(lenEncrypted);
		byte[] decryptedCon = secCipher.doFinal(conEncrypted);
		//Separate the msg and Digest
		byte[] destinationFileName = Arrays.copyOfRange(decryptedDFN, 0, decryptedDFN.length-digestLength);
		byte[] hmacHashDFN = Arrays.copyOfRange(decryptedDFN, decryptedDFN.length-digestLength, decryptedDFN.length);
		byte[] lenInByteArr = Arrays.copyOfRange(decryptedLen, 0, decryptedLen.length-digestLength);
		byte[] hmacHashLen = Arrays.copyOfRange(decryptedLen, decryptedLen.length-digestLength, decryptedLen.length);
		byte[] content = Arrays.copyOfRange(decryptedCon, 0, decryptedCon.length-digestLength);
		byte[] hmacHashCon = Arrays.copyOfRange(decryptedCon, decryptedCon.length-digestLength, decryptedCon.length);
		
		//Generate the hash for the decrypted file
		byte[] hmacHashDFNCheck = mac.doFinal(destinationFileName);
		byte[] hmacHashLenCheck = mac.doFinal(lenInByteArr);
		byte[] hmacHashConCheck = mac.doFinal(content);
		
		//Check is the digest are equal
		boolean integrityCheck = false;
		if((Arrays.equals(hmacHashDFNCheck, hmacHashDFN)) || 
				(Arrays.equals(hmacHashLenCheck, hmacHashLen)) || 
				(Arrays.equals(hmacHashConCheck, hmacHashCon))) {
			if(parent.getDebug()){
				System.out.println("The message was not tampered.");
				System.out.println("DFN: " + new String(destinationFileName));
				System.out.println("Content: " + new String(content));
			}
			integrityCheck = true;
		} else {
			if(parent.getDebug()){
				System.out.println("WARNING: MESSAGE WAS TAMPERED.");
			}
		}
		//Write the contents to the destination file
		if(integrityCheck) {
			FileOutputStream msgFile = new FileOutputStream(new String(destinationFileName));
			msgFile.write(content);
			msgFile.close();
			//Send acknowledgement to the Client Program
			writer.flush();
			writer.writeInt(12); //12 - means that there was no tampering
			System.out.println("Transfer Succesful Without Tampering.");
		} else {
			if(!(Arrays.equals(hmacHashDFNCheck, hmacHashDFN))){
				writer.writeInt(0); // 0 - Destination Filename has been tampered
				System.out.println("Destination File Name protocol has been tampered.");
			} else if (!(Arrays.equals(hmacHashLenCheck, hmacHashLen))) {
				writer.writeInt(1); // 1 - Content Length has been tampered
				System.out.println("Content Length protocol has been tampered.");
			} else if (!(Arrays.equals(hmacHashConCheck, hmacHashCon))) {
				writer.writeInt(2); // 2 - Content has been Tampered
				System.out.println("Content protocol has been tampered.");
			}
		}
		} catch (IOException e) {
			if (parent.getFlag()){
				System.out.println ("shutting down.");
				return;
			}
			return;
		} catch (Exception e){
			e.printStackTrace();
			parent.killall();
			return;
		}
     }
 }
