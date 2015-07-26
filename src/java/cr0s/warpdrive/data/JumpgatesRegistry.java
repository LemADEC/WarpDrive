package cr0s.warpdrive.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import cr0s.warpdrive.WarpDrive;

public final class JumpgatesRegistry {
    private File db;
    private ArrayList<Jumpgate> gates = new ArrayList<Jumpgate>();

    //@SideOnly(Side.CLIENT)
    public JumpgatesRegistry() {
        db = new File("gates.txt");
        WarpDrive.print("Gates.txt file: " + db);
        
        if (db != null && !db.exists()) {
        	try {
				db.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        try {
            loadGates();
        } catch (IOException ex) {
            Logger.getLogger(JumpgatesRegistry.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveGates() throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(db));

        // Write each string in the array on a separate line
        for (Jumpgate jg : gates) {
            out.println(jg);
        }

        out.close();
    }

    public void loadGates() throws IOException {
    	WarpDrive.print("[JUMP GATES] Loading jump gates from gates.txt...");
        BufferedReader bufferedreader;
        bufferedreader = new BufferedReader(new FileReader(db));
        String s1;

        while ((s1 = bufferedreader.readLine()) != null) {
            gates.add(new Jumpgate(s1));
        }

        bufferedreader.close();
        WarpDrive.print("[JUMP GATES] Loaded " + gates.size() + " jump gates.");
    }

    public void addGate(Jumpgate jg) {
        gates.add(jg);
    }

    public boolean addGate(String name, int x, int y, int z) {
        // Gate already exists
        if (findGateByName(name) != null) {
            return false;
        }

        addGate(new Jumpgate(name, x, y, z));

        try {
            saveGates();
        } catch (IOException ex) {
            Logger.getLogger(JumpgatesRegistry.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    public void removeGate(String name) {
        Jumpgate jg;

        for (int i = 0; i < gates.size(); i++) {
            jg = gates.get(i);

            if (jg.name.equalsIgnoreCase(name))
            {
                gates.remove(i);
                return;
            }
        }

        try {
            saveGates();
        } catch (IOException ex) {
            Logger.getLogger(JumpgatesRegistry.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Jumpgate findGateByName(String name) {
        for (Jumpgate jg : gates) {
            if (jg.name.equalsIgnoreCase(name)) {
                return jg;
            }
        }

        return null;
    }

    public String JumpgatesList() {
        String result = "";

        for (Jumpgate jg : gates) {
            result += jg.toNiceString() + "\n";
        }

        return result;
    }
    
    public String commaList() {
    	String result = "";
    	for (Jumpgate jg : gates) {
    		result += jg.toNiceString() + ",";
    	}
    	return result;
    }

    public Jumpgate findNearestGate(int x, int y, int z) {
//    	WarpDrive.debugPrint(JumpgatesList());
        double minDistance2 = -1;
        Jumpgate res = null;

        for (Jumpgate jg : gates) {
            double dX = jg.xCoord - x;
            double dY = jg.yCoord - y;
            double dZ = jg.zCoord - z;
            double distance2 = dX * dX + dY * dY + dZ * dZ;

            if ((minDistance2 == -1) || (distance2 < minDistance2)) {
                minDistance2 = distance2;
                res = jg;
            }
        }

        return res;
    }
}
