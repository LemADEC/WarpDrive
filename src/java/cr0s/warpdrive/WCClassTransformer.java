package cr0s.warpdrive;

import java.util.HashMap;
import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class WCClassTransformer implements net.minecraft.launchwrapper.IClassTransformer {
	private HashMap<String, String> nodemap = new HashMap<String, String>();

	private final String GRAVITY_MANAGER_CLASS = "cr0s/WarpDrive/GravityManager";

	public WCClassTransformer() {
		nodemap.put("worldClass", "abw");
		nodemap.put("playerMP", "jv");
		nodemap.put("netLoginHandler", "jy");
		nodemap.put("confManagerClass", "hn");
		nodemap.put("createPlayerMethod", "a");
		nodemap.put("createPlayerDesc", "(Ljava/lang/String;)L" + nodemap.get("playerMP") + ";");
		nodemap.put("respawnPlayerMethod", "a");
		nodemap.put("respawnPlayerDesc", "(L" + nodemap.get("playerMP") + ";IZ)L" + nodemap.get("playerMP") + ";");
		nodemap.put("itemInWorldManagerClass", "jw");
		nodemap.put("attemptLoginMethodBukkit", "attemptLogin");
		nodemap.put("attemptLoginDescBukkit", "(L" + nodemap.get("netLoginHandler") + ";Ljava/lang/String;Ljava/lang/String;)L" + nodemap.get("playerMP") + ";");
		nodemap.put("playerControllerClass", "bdc");
		nodemap.put("playerClient", "bdi");
		nodemap.put("netClientHandler", "bcw");
		nodemap.put("createClientPlayerMethod", "a");
		nodemap.put("createClientPlayerDesc", "(L" + nodemap.get("worldClass") + ";)L" + nodemap.get("playerClient") + ";");
		nodemap.put("entityLivingBaseClass", "of");
		nodemap.put("moveEntityMethod", "e");
		nodemap.put("moveEntityDesc", "(FF)V");
		nodemap.put("entityItemClass", "ss");
		nodemap.put("onUpdateMethod", "l_");
		nodemap.put("onUpdateDesc", "()V");
		nodemap.put("entityRendererClass", "bfe");
		nodemap.put("updateLightmapMethod", "h");
		nodemap.put("updateLightmapDesc", "(F)V");
		nodemap.put("player", "uf");
		nodemap.put("containerPlayer", "vv");
		nodemap.put("invPlayerClass", "ud");
		nodemap.put("minecraft", "atv");
		nodemap.put("session", "aus");
		nodemap.put("guiPlayer", "axv");
		nodemap.put("thePlayer", "h");
		nodemap.put("displayGui", "a");
		nodemap.put("guiScreen", "awe");
		nodemap.put("displayGuiDesc", "(L" + nodemap.get("guiScreen") + ";)V");
		nodemap.put("runTick", "k");
		nodemap.put("runTickDesc", "()V");
		nodemap.put("clickMiddleMouseButton", "W");
		nodemap.put("clickMiddleMouseButtonDesc", "()V");
		nodemap.put("itemRendererClass", "bfj");
		nodemap.put("renderOverlaysMethod", "b");
		nodemap.put("renderOverlaysDesc", "(F)V");
		nodemap.put("updateFogColorMethod", "i");
		nodemap.put("updateFogColorDesc", "(F)V");
		nodemap.put("getFogColorMethod", "f");
		nodemap.put("getSkyColorMethod", "a");
		nodemap.put("vecClass", "atc");
		nodemap.put("entityClass", "nn");
		nodemap.put("getFogColorDesc", "(F)L" + nodemap.get("vecClass") + ";");
		nodemap.put("getSkyColorDesc", "(L" + nodemap.get("entityClass") + ";F)L" + nodemap.get("vecClass") + ";");
		nodemap.put("guiSleepClass", "avm");
		nodemap.put("wakeEntityMethod", "g");
		nodemap.put("wakeEntityDesc", "()V");
		nodemap.put("orientCameraDesc", "(L" + nodemap.get("minecraft") + ";L" + nodemap.get("entityLivingBaseClass") + ";)V");
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if (nodemap == null) {
			System.out.println("========= NODEMAP IS NULL!!! ========");
			return bytes;
		}

		if (name.replace('.', '/').equals(nodemap.get("entityLivingBaseClass"))) {
			bytes = transformEntityLivingBase(bytes);
		} else if (name.replace('.', '/').equals(nodemap.get("entityItemClass"))) {
			bytes = transformEntityItem(bytes);
		}

		return bytes;
	}

	private byte[] transformEntityItem(byte[] bytes) {
		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytes);
		reader.accept(node, 0);
		int operationCount = 2;
		int injectionCount = 0;
		Iterator methods = node.methods.iterator();

		do {
			if (!methods.hasNext()) {
				break;
			}

			MethodNode methodnode = (MethodNode) methods.next();

			if (methodnode.name.equals(nodemap.get("onUpdateMethod")) && methodnode.desc.equals(nodemap.get("onUpdateDesc"))) {
				int count = 0;

				while (count < methodnode.instructions.size()) {
					AbstractInsnNode list = methodnode.instructions.get(count);

					if (list instanceof LdcInsnNode) {
						LdcInsnNode nodeAt = (LdcInsnNode) list;

						if (nodeAt.cst.equals(Double.valueOf(0.039999999105930328D))) {
							VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							MethodInsnNode overwriteNode = new MethodInsnNode(Opcodes.INVOKESTATIC, GRAVITY_MANAGER_CLASS, "getItemGravity", "(L"
									+ nodemap.get("entityItemClass") + ";)D");
							methodnode.instructions.insertBefore(nodeAt, beforeNode);
							methodnode.instructions.set(nodeAt, overwriteNode);
							injectionCount++;
						}

						if (nodeAt.cst.equals(Double.valueOf(0.98000001907348633D))) {
							VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							MethodInsnNode overwriteNode = new MethodInsnNode(Opcodes.INVOKESTATIC, GRAVITY_MANAGER_CLASS, "getItemGravity2", "(L"
									+ nodemap.get("entityItemClass") + ";)D");
							methodnode.instructions.insertBefore(nodeAt, beforeNode);
							methodnode.instructions.set(nodeAt, overwriteNode);
							injectionCount++;
						}
					}

					count++;
				}
			}
		} while (true);

		ClassWriter writer = new ClassWriter(1);
		node.accept(writer);
		bytes = writer.toByteArray();
		System.out.println("[WDCore] WarpDrive successfully injected bytecode into: " + node.name + " (" + injectionCount + " / " + operationCount + ")");
		return bytes;
	}

	private byte[] transformEntityLivingBase(byte[] bytes) {
		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytes);
		reader.accept(node, 0);
		int operationCount = 1;
		int injectionCount = 0;
		Iterator methods = node.methods.iterator();

		do {
			if (!methods.hasNext()) {
				break;
			}

			MethodNode methodnode = (MethodNode) methods.next();

			if (methodnode.name.equals(nodemap.get("moveEntityMethod")) && methodnode.desc.equals(nodemap.get("moveEntityDesc"))) {
				int count = 0;

				while (count < methodnode.instructions.size()) {
					AbstractInsnNode list = methodnode.instructions.get(count);

					if (list instanceof LdcInsnNode) {
						LdcInsnNode nodeAt = (LdcInsnNode) list;

						if (nodeAt.cst.equals(Double.valueOf(0.080000000000000002D))) {
							VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							MethodInsnNode overwriteNode = new MethodInsnNode(Opcodes.INVOKESTATIC, GRAVITY_MANAGER_CLASS, "getGravityForEntity", "(L"
									+ nodemap.get("entityLivingBaseClass") + ";)D");
							methodnode.instructions.insertBefore(nodeAt, beforeNode);
							methodnode.instructions.set(nodeAt, overwriteNode);
							injectionCount++;
						}
					}

					count++;
				}
			}
		} while (true);

		ClassWriter writer = new ClassWriter(1);
		node.accept(writer);
		bytes = writer.toByteArray();
		System.out.println("[WDCore] WarpDrive successfully injected bytecode into: " + node.name + " (" + injectionCount + " / " + operationCount + ")");
		return bytes;
	}
}
