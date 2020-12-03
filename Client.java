/**
 *
 * @author Sidney Shane Dizon
 * UCID: 10149277
 * Course: CPSC 418 - Assignment 2
 * Description:
 * 	This Client program prompts the user for shared key at startup, to be used as the seed in
 * 	a pseudorandom number generator. It also prompts the user for source and destination file names.
 * 	The destination file name, length of the source file in bytes and the source file contents are then
 * 	transfered to the Server Program. This Client Program will exit after receiving acknowledgement from
 * 	the Server Program.
 *
 *	Input as Command-line argument:
 *		i) IP - IP address of the server
 *	   ii) Port - port number for socket connection
 */
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Client {
	private Socket sock; //socket to communicate with
	private boolean debugging; //debug flag
	private static KeyGenerator keyGen = null;
	private static KeyGenerator hmacKey = null;
	private static SecretKey secKey = null;
 	private static byte[] raw = null;
    private static SecretKeySpec secKeySpec = null;
    private static Cipher secCipher = null;

	/**
	 * Main method, starts the client.
	 * @param args args[0] needs to be a hostname, args[1] a port number, args[2] debugging on/off
	 */
	public static void main(String[] args) {
		
		if (args.length < 2) {
			System.out.println ("Usage: java Client hostname port#");
	    System.out.println ("hostname is a string identifying your server");
	    System.out.println ("port is a positive integer identifying the port to connect to the server");
	    return;
		}
		boolean checkdebug = false;
		if (args.length == 3){
			if (args[2].compareTo("debug") == 0){
				checkdebug = true;
			}
		}

		try{
			Client c = new Client (args[0], Integer.parseInt(args[1]), checkdebug);
		} catch (NumberFormatException e){
			System.out.println ("Usage: java Client hostname port#");
	    System.out.println ("Second argument was not a port number");
	    return;
		}
	}





	/**
	 * Constructor, in this case does everything.
	 * @param ipaddress The hostname to connect to.
	 * @param port The port to connect to.
	 * @param bool The debug on/off flag
	 */
	 public Client (String ipAddress, int port, boolean bool){
		 byte[] hmacHashDFN = null;
		 byte[] hmacHashContent = null;
		 byte[] hmacHashLength = null;
		 FileInputStream messageFile = null;
		 DataOutputStream writer = null;
		 DataInputStream input = null;
		
		 debugging = bool;
		 if(debugging){
			 System.out.println("Debugging is on for the Client program.");
		 }

		 /* Allows us to get input from the Keyboard */
		 BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		 String userInput, sourceFileName, destinationFileName;
		 String sharedKey;
		 

		 /* try to connect to the specified host on the specified port */
		 try{
			 sock = new Socket (InetAddress.getByName(ipAddress), port);
			 
		 } catch (UnknownHostException e){
			 System.out.println ("Usage: java Client hostname port#");
	    System.out.println ("First argument is not a valid hostname");
	    return;
		} catch (IOException e){
			System.out.println ("Could not connect to " + ipAddress + ".");
	    return;
		}
		/* Status info */
		System.out.println ("Connected to " + sock.getInetAddress().getHostAddress() + " on port " + port);

		try{
			writer = new DataOutputStream(sock.getOutputStream());
			input = new DataInputStream(sock.getInputStream());
			
		} catch (IOException e){
			System.out.println ("Could not create output stream.");
	    return;
		}

		/* Wait for the user to type stuff. */
		try{
			//Prompt the user for the shared key, source and destination File
			System.out.println("Please type in the shared key: ");
			sharedKey = stdIn.readLine();
			System.out.println("Please type in the source file name:");
			sourceFileName = stdIn.readLine();
			System.out.println("Please type in the destination File name:");
			destinationFileName = stdIn.readLine();
			
			messageFile = new FileInputStream(sourceFileName);
			byte[] content = new byte[messageFile.available()];
			messageFile.read(content);	
			
			/* Do the PNG so that we can generate 128-bit session key */
		    //SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		    SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		    random.setSeed(sharedKey.getBytes());
		    if(debugging){
		    	byte[] result = new byte[128/8];
		    	random.nextBytes(result);
		      System.out.println("SecureRandom: " + result);
		    }
		    //Generate HMACKey
			hmacKey = KeyGenerator.getInstance("HMACSHA1");
			hmacKey.init(128, random);
			SecretKey secretKey = hmacKey.generateKey();
			if(debugging){
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

			//AES Using CBC - Generate IV
			byte[] iv = new byte[128/8];
			random.nextBytes(iv);

			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			//HMAC SHA-1 CBC Hash
			hmacHashDFN = mac.doFinal(destinationFileName.getBytes());
			hmacHashLength = mac.doFinal(ByteBuffer.allocate(4).putInt(sourceFileName.length()).array());
			hmacHashContent = mac.doFinal(content);
			if(debugging){
		        System.out.println("HMACHASHDFN Generated: " + hmacHashDFN + " : " + hmacHashDFN.length);
		        System.out.println("HMACHASHLength Generated: " + hmacHashLength + " : " + hmacHashLength.length);
		        System.out.println("HMACHASHContent Generated: " + hmacHashContent + " : " + hmacHashContent.length);
		      }
			//Concatenate the digest with the messages
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			b.write(destinationFileName.getBytes());
			b.write(hmacHashDFN);
			byte[] dfnNDigest = b.toByteArray();
			
			b.reset();
			b.write(ByteBuffer.allocate(4).putInt(sourceFileName.length()).array());
			b.write(hmacHashLength);
			byte[] lengNDigest = b.toByteArray();
			
			b.reset();
			b.write(content);
			b.write(hmacHashContent);
			byte[] contentNDigest = b.toByteArray();
			
			/* Encrypt the File with AES */
			secCipher.init(Cipher.ENCRYPT_MODE, secKeySpec, ivSpec);
			byte[] encryptedDFN = secCipher.doFinal(dfnNDigest);
			byte[] encryptedLength = secCipher.doFinal(lengNDigest);
			byte[] encryptedContent = secCipher.doFinal(contentNDigest);
			if(debugging){
		        System.out.println("Encrypted DFN: " + encryptedDFN + ": " + encryptedDFN.length);
		        System.out.println("Encrypted Length: " + encryptedLength + ": " + encryptedLength.length);
		        System.out.println("Encrypted Content: " + encryptedContent + ": " + encryptedContent.length);
		      }
			
			writer.writeInt(encryptedDFN.length);
			
			writer.write(encryptedDFN, 0, encryptedDFN.length);
			writer.flush();
			writer.writeInt(encryptedLength.length);
			writer.flush();
			writer.write(encryptedLength, 0, encryptedLength.length);
			writer.flush();
			writer.writeInt(encryptedContent.length);
			writer.flush();
			writer.write(encryptedContent, 0, encryptedContent.length);

			int ack = input.readInt();
			if(debugging) {
				System.out.println("Server Ack: " + ack);
			}
			if(ack == 12) {
				System.out.println("Transfer Successful without Tampering");
			} else if (ack == 0) { // 0 - Destination Filename has been tampered
				System.out.println("Destination File Name has been tampered");
			} else if (ack == 1) { // 1 - Content Length has been tampered
				System.out.println("Content Length has been tampered");
			} else if (ack == 2) { // 2 - Content has been Tampered
				System.out.println("Content has been tampered");
			} 
			
			/* Close */
			stdIn.close();
		    writer.close();
		    sock.close();
		    return;
		} catch (IOException e){
			e.printStackTrace();
			return;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return;
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return;
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return;
		}
	}
}
