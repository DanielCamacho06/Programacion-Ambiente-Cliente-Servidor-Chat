import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainCharServidorSusurro {

    private static Set<String> names = new HashSet<>();

    private static Set<PrintWriter> writers = new HashSet<>();
    
    //Map se utiliza para guardar el nombre de los clientes y la ruta en la que se le envía el susurro
    private static Map<String,PrintWriter> susurros = new HashMap<>();
        
    public static void main(String[] args)  throws Exception{
        System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try(ServerSocket listener = new ServerSocket(59001)){
            while(true){
                pool.execute(new Handler(listener.accept())  {});
            }
        }
    }//FIN MAIN
    
    private static class Handler implements Runnable{
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;
        
        public Handler(Socket socket){
            this.socket = socket;
        }
        
        public void run(){
            try{
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(),true);

                while(true){
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if(name == null){
                        return;
                    }
                    if(name.equalsIgnoreCase("quit")){
                        return;
                    }
                    if(name.equalsIgnoreCase("block")){
                        return;
                    }
                    if(name.equalsIgnoreCase(name.substring(name.indexOf(" ")))){
                        return;
                    }
                    synchronized (names) {
                        if(!names.contains(name)){
                            names.add(name);
                            susurros.put(name, out);
                            break;
                        }
                    }
                }
                
                out.println("NAMEACCEPTED " + name);
                for(PrintWriter writer : writers){
                    writer.println("MESSAGE " + name + " has joined");
                }
                
                writers.add(out);
                
                while(true){
                    String input = in.nextLine();
                    
                    if(input.toLowerCase().startsWith("/")){
                        if(input.toLowerCase().startsWith("/quit")){
                            return;
                        }
                        
                        String nombreSusurro = input.substring(1,input.indexOf(" "));

                        if(input.toLowerCase().startsWith("/") && names.contains(nombreSusurro)){
                            susurros.get(nombreSusurro).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" ")) + " whispering");
                            susurros.get(name).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" ")) + " whispering");
                        }
                    }else{
                        for(PrintWriter writer : writers){
                            writer.println("MESSAGE " + name + " : " + input);
                        }
                    }

                }
            
            }catch(Exception e){
                System.out.println(e);
            }finally {
                if(out != null){
                    writers.remove(out);
                }
                if(name != null){
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    for(PrintWriter writer : writers){
                        writer.println("MESSAGE " + name + " has left...");
                    }
                }
                try{ socket.close(); } catch(IOException e){ System.out.println(e); }
            }
            
        }//FIN RUN
        
    }//FIN HANDLER
    
}//FIN CLASS