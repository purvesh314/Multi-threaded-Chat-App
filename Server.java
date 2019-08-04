// Java implementation of Server side 
// It contains two classes : Server and ClientHandler 
// Save file as Server.java 

import java.io.*; 
import java.util.*; 
import java.net.*; 
import java.sql.*;

// Server class 
public class Server 
{
	static String url="jdbc:mysql://localhost:3306/";
	static String dbName="te3157db";
	static String driver="com.mysql.jdbc.Driver";
	static String username="pj";
	static String password="1234";
	static Connection conn=null;
	static Statement st=null;
	static int port=0;


	// Vector to store active clients 
	static Vector<ClientHandler> ar = new Vector<>(); 
	
	// counter for clients 
	static int i = 0; 

	public static void main(String[] args) throws IOException 
	{
		try
		{ 
			// server is listening on port 1234 
			ServerSocket ss = new ServerSocket(1234); 
			
			Socket s; 
			
			Class.forName(driver);
			System.out.println("Connection : "+conn);
			conn=DriverManager.getConnection(url+dbName,username,password);
			System.out.println("Connection : "+conn);
			st=conn.createStatement();

			// running infinite loop for getting 
			// client request 
			while (true) 
			{ 
				// Accept the incoming request 
				s = ss.accept(); 
				// obtain input and output streams 
				DataInputStream dis = new DataInputStream(s.getInputStream()); 
				DataOutputStream dos = new DataOutputStream(s.getOutputStream()); 
				
				System.out.println("New client request received : " + s); 
				
				//Login begins
				String name,password;
				name=dis.readUTF();
				password=dis.readUTF();
				System.out.println("Here");
				try {
					PreparedStatement ps=conn.prepareStatement("select * from users where name=? and password=?");
					ps.setString(1, name);
					ps.setString(2, password);

					ResultSet rs=ps.executeQuery();
					System.out.println("tHere");
					//ps.close();
					if(rs.next())
					{
						dos.writeUTF("1");
					}
					else
					{
						dos.writeUTF("0");
					}
				}
				catch (SQLException e) {
					e.printStackTrace();
				}
				//Login ends

				
				System.out.println("Creating a new handler for this client..."); 

				// Create a new handler object for handling this request. 
				ClientHandler mtch = new ClientHandler(s,name, dis, dos,conn,st); 

				// Create a new Thread with this object. 
				Thread t = new Thread(mtch); 
				
				System.out.println("Adding this client to active client list"); 

				// add this client to active clients list 
				ar.add(mtch); 

				// start the thread. 
				t.start(); 

				// increment i for new client. 
				// i is used for naming only, and can be replaced 
				// by any naming scheme 
				i++; 

			}
		}
		catch(Exception e)
		{} 
	}

	 
} 

// ClientHandler class 
class ClientHandler implements Runnable 
{ 
	Scanner scn = new Scanner(System.in); 
	private String name; 
	final DataInputStream dis; 
	final DataOutputStream dos; 
	Socket s; 
	boolean isloggedin; 

	static Connection conn=null;
	static Statement st=null;
	
	// constructor 
	public ClientHandler(Socket s, String name, 
							DataInputStream dis, DataOutputStream dos, Connection conn, Statement st) { 
		this.dis = dis; 
		this.dos = dos; 
		this.name = name; 
		this.s = s; 
		this.isloggedin=true;

		this.conn=conn;
		this.st=st; 
	} 

	@Override
	public void run() { 

		String received; 
		while (true) 
		{ 
			try
			{ 
				// receive the string 
				received = dis.readUTF(); 
				
				System.out.println(received); 
				
				if(received.equals("logout")){ 
					this.isloggedin=false; 
					this.s.close(); 
					break; 
				} 
				
				// break the string into message and recipient part 
				StringTokenizer st = new StringTokenizer(received, "@"); 
				String MsgToSend = st.nextToken(); 
				String recipient = st.nextToken(); 

				//Chat history
				if(MsgToSend.equals("history"))
				{
					System.out.println("In history");
					try
					{
						PreparedStatement ps=conn.prepareStatement("select * from messages where (sender=? and receiver=?) or (sender=? and receiver=?)");				
						ps.setString(1,this.name);
						ps.setString(2,recipient);
						ps.setString(3,recipient);
						ps.setString(4,this.name);
						ResultSet res=ps.executeQuery();
						while(res.next())
						{
							String sd=res.getString("sender");
							String msg=res.getString("message");
							System.out.println(sd+" : "+msg);
							dos.writeUTF(sd+" : "+msg);
						}
						ps.close();
					}
					catch(SQLException e)
					{
						e.printStackTrace();
					}	
				}

				// search for the recipient in the connected devices list. 
				// ar is the vector storing client of active users 
				for (ClientHandler mc : Server.ar) 
				{ 
					// if the recipient is found, write on its 
					// output stream 
					if (mc.name.equals(recipient) && mc.isloggedin==true) 
					{ 
						mc.dos.writeUTF(this.name+" : "+MsgToSend); 

						try
						{
							PreparedStatement ps=conn.prepareStatement("insert into messages (sender,receiver,message) values(?,?,?)");
							ps.setString(1,this.name);
							ps.setString(2,recipient);
							ps.setString(3,MsgToSend);

							ps.executeUpdate();
							ps.close();
						}
						catch(SQLException e)
						{
							e.printStackTrace();
						}

						break; 
					} 
				} 
			} catch (IOException e) { 
				
				e.printStackTrace(); 
			} 
			
		} 
		try
		{ 
			// closing resources 
			this.dis.close(); 
			this.dos.close(); 
			
		}catch(IOException e){ 
			e.printStackTrace(); 
		} 
	} 
} 
