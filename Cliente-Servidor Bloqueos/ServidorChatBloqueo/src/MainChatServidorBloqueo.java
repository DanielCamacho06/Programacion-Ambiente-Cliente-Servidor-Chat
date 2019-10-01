import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainChatServidorBloqueo {

    private static Set<String> names = new HashSet<>();

    private static Set<PrintWriter> writers = new HashSet<>();
    
    //Map que se utiliza para guardar el nombre de los clientes y la ruta
    private static Map<String,PrintWriter> ruta = new HashMap<>();
    
    //Map que se utiliza para guardar a los usuario bloqueados. String usuario Set usuarios que te tienen bloqueado
    private static Map<String, Set> bloqueos = new HashMap<>();
    
    public static void main(String[] args)  throws Exception{
        System.out.println("The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try(ServerSocket listener = new ServerSocket(59001)){
            while(true){
                pool.execute(new Handler(listener.accept())  {});
            }
        }
    }//FIN MAIN

    public static Set blockSet(Set set,String usuarioBloqueado){
        if(!names.contains(usuarioBloqueado)){
            return set = null;
        }else{
            if(set != null){
                set.add(usuarioBloqueado);
            }
            else{
                set = new HashSet<>();
                set.add(usuarioBloqueado);
            }
            return set;
        }
    }
    
    public static Set unBlockSet(Set set,String usuarioDesBloqueado){
        if(set != null){
            set.remove(usuarioDesBloqueado);
        }
        return set;
    }
    
    public static void susurro(String nombreUsuarioSusurro, String name, String input){
        ruta.get(nombreUsuarioSusurro).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" ")) + " whispering you");
        ruta.get(name).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" ")) + " whispering to " + nombreUsuarioSusurro);
    }
    
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
                    synchronized (names) {
                        if(!names.contains(name)){
                            names.add(name);
                            ruta.put(name, out);
                            bloqueos.put(name, null);
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
                        }else{
                            if(input.toLowerCase().startsWith("/block")){
                                String nombreUsuarioBloquear = input.substring(input.indexOf(" ") + 1);
                                System.out.println("1");
                                System.out.println(nombreUsuarioBloquear);
                                if(names.contains(nombreUsuarioBloquear)){
                                    System.out.println("2");
                                    Set set = blockSet(bloqueos.get(nombreUsuarioBloquear),name);
                                    if(set != null){
                                        System.out.println("3");
                                        bloqueos.put(nombreUsuarioBloquear, set);
                                        System.out.println(bloqueos.get(nombreUsuarioBloquear) + "bloqueaste a " + nombreUsuarioBloquear);
                                    }   
                                }
                            }else{
                                if( input.toLowerCase().startsWith("/unblock")){
                                    String nombreUsuarioDesBloquear = input.substring(input.indexOf(" ") + 1);
                                    
                                    if(names.contains(nombreUsuarioDesBloquear)){
                                        bloqueos.replace(nombreUsuarioDesBloquear, unBlockSet(bloqueos.get(nombreUsuarioDesBloquear), name));
                                    }
                                }else{
                                    if(input.indexOf(" ") != -1){
                                        String nombreUsuarioSusurro = input.substring(1,input.indexOf(" "));
                                        if(input.toLowerCase().startsWith("/") && names.contains(nombreUsuarioSusurro)){
                                            if(bloqueos.get(name) == null){
                                                susurro(nombreUsuarioSusurro, name, input);
                                            }else{
                                                if(!bloqueos.get(name).contains(nombreUsuarioSusurro)){
                                                    susurro(nombreUsuarioSusurro, name, input);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else{                       
                        Set msj = ruta.entrySet();                        
                        Iterator iterator = msj.iterator();
                        while(iterator.hasNext()){
                            Map.Entry mentry = (Map.Entry)iterator.next();
                            //System.out.println("1: " + bloqueos.get(name) + " 2: " + mentry.getKey());
                            if(bloqueos.get(name) != null){
                                if(!bloqueos.get(name).contains(mentry.getKey())){
                                    PrintWriter pw = (PrintWriter)mentry.getValue();
                                    pw.println("MESSAGE " + name + " : " + input);
                                }
                            }else{
                                PrintWriter pw = (PrintWriter)mentry.getValue();
                                pw.println("MESSAGE " + name + " : " + input);
                            }

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
