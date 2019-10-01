import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

public class MainServidorChatPersistente {

    private static Set<String> names = new HashSet<>();

    private static Set<PrintWriter> writers = new HashSet<>();
    
    //Map que se utiliza para guardar el nombre de los clientes y la ruta
    private static Map<String,PrintWriter> ruta = new HashMap<>();
    
    //Map que se utiliza para guardar a los usuario bloqueados. String usuario Set usuarios que te tienen bloqueado
    private static Map<String, Set> bloqueos = new HashMap<>();
    
    public static void main(String[] args)  throws Exception{
        HashMap<String, Set> map = null;
        boolean band = false;
        try{
            FileInputStream fis = new FileInputStream("block.conf");
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap) ois.readObject();
            bloqueos = map;
            ois.close();
            fis.close();
        }catch(IOException e){
            band = true;
        }
        
        
        if(band == false){
            System.out.println("Deserialized HashMap..");
            //Display content using Iterator
            Set set = bloqueos.entrySet();
            Iterator iterator = set.iterator();
            while(iterator.hasNext()) {
               Map.Entry mentry = (Map.Entry)iterator.next();
               System.out.print("key: "+ mentry.getKey() + " & Value: ");
               System.out.println(mentry.getValue());
            }  
        }    

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
                            if(!bloqueos.containsKey(name)){
                                bloqueos.put(name, null);
                            }
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
                    
                    try{
                        FileOutputStream fos = new FileOutputStream("block.conf");
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(bloqueos);
                        oos.close();
                        fos.close();
                        System.out.printf("Información de HashMap 'bloqueos' guardada en block.conf");
                    }catch(IOException ioe){
                        ioe.printStackTrace();
                    }
                }
                try{ socket.close(); } catch(IOException e){ System.out.println(e); }
            }
            
        }//FIN RUN
        
    }//FIN HANDLER
    
}//FIN CLASS