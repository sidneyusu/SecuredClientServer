Author: Sidney Shane Dizon
UCID: 10149277
Course: CPSC 418 - Assignment 2 Problem 6

Descriptions:
    -List of Files Submitted with description:
        -Server.java
            This Server program prompts the user for shared key at startup, to be used as the seed in 
            a pseudorandom number generator. The Server Program decrypts the file, check the integrity 
            of the file content, and write the file content to the destination file specified by the client.
            This sends an acknowledgement to the client reporting success if the file was correctly decrypted, 
            passed the integrity check and written to the destination file; otherwise it will report failure.
         
            The Server echoes any keyboard input obtained by the client. 
            The program allows a command line argument that defines whether debugging is on or off for the whole program.
                -Enabled
                    -- your program should echo all protocol messages sent and received in such a way that is is clearly
                        identified which program instance, the call for Server program would be
                    -- If the 2nd argument in args[] is present and equal to 'debug', then the debug flag is set in the program.
        -Client.java
            This Client program prompts the user for shared key at startup, to be used as the seed in 
            a pseudorandom number generator. It also prompts the user for source and destination file names.
            The destination file name, length if the source file in bytes and the source file contents are then 
            transfered to the Server Program. This Client Program will exit after receiving acknowledgement from
            the Server Program.
        -ServerThread.java
            This ServerThread Program contains a class for the thread to deal with clients who connect 
            to the Server Program.
    -How to Compile:
        -javac Server.java 
        -javac Client.java
 
    -How to test the programs:
        -Name of Server: 
            -linux.cpsc.ucalgary.ca
        -Test the Program:
            -java Server 80 or java Server 80 debug or java Server 80 something
                -input the shared key to be used for the PNG
            -java Client 0.0.0.0 80 or java Client 0.0.0.0 80 debug or java Client 0.0.0.0 80 something
                -input the shared key to be used for the PNG
                -input the source and destination filenames
    -The Problem is solved in full 
    -There are no known bugs
    -File Transfer Protocol:
        -Description of Protocol Messages with format:
            -There are 3 Protocol Messages from the Client Program:
                -Destination Filename
                    -- Enc(destinationFileName|HmacHashDestinationFileName)
                        -Encrypted with AES-128-CBC
                -Content Length
                    -- Enc(Length|HmacHashLength)
                    -Encrypted with AES-128-CBC
                -Content
                    -- Enc(Content|HmacHashContent)
                    -Encrypted with AES-128-CBC
            -There is 1 Protocol Message from the Server Program:
                -Acknowledgement
                    -12 if the tranfer was success and no tampering
                    -0 if the destination file name was tampered
                    -1 if the content length was tampered
                    -2 if the content was tampered 
        -How they are parsed upon receipt
            -digestLength = 20
            -byte[] destinationFileName = Arrays.copyOfRange(decryptedDFN, 0, decryptedDFN.length-digestLength);
            -byte[] hmacHashDFN = Arrays.copyOfRange(decryptedDFN, decryptedDFN.length-digestLength, decryptedDFN.length);
            -byte[] lenInByteArr = Arrays.copyOfRange(decryptedLen, 0, decryptedLen.length-digestLength);
            -byte[] hmacHashLen = Arrays.copyOfRange(decryptedLen, decryptedLen.length-digestLength, decryptedLen.length);
            -byte[] content = Arrays.copyOfRange(decryptedCon, 0, decryptedCon.length-digestLength);
            -byte[] hmacHashCon = Arrays.copyOfRange(decryptedCon, decryptedCon.length-digestLength, decryptedCon.length);
        -How Encryption and Data Integrity are employed
            -Fields that are protected
                -Destination Filename that is sent to the Server from the Client
                    -Integrity was check by comparing the generated HMACHASH of the Server to the one sent by the Client 
                    -Data Confidentiality was maintained through AES-128-CBC Encryption
                -Content Length that is sent to the Server from the Client
                    -Integrity was check by comparing the generated HMACHASH of the Server to the one sent by the Client 
                    -Data Confidentiality was maintained through AES-128-CBC Encryption
                -Content that is sent to the Server from the Client
                    -Integrity was check by comparing the generated HMACHASH of the Server to the one sent by the Client 
                    -Data Confidentiality was maintained through AES-128-CBC Encryption
        -How attacks are prevented on confidentiality and data integrity above
            -Active Attack such as below are prevented:                -
                -Tampered Protocol Message to the Server was prevented through checking of the generated HMAC by the Server 
                -Generation of the a new HMAC for the message was prevented through the use of PRNG and seed from the user