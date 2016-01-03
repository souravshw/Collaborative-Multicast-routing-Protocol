package core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CreateFile {
	String filePath = "/home/sanu/Copy/workspace/FOne/src/FilesOutput/";
	String fileName = null;
	File file = null;
	// private FileWriter fw=null;
	private BufferedWriter bw = null;

	public CreateFile(String name) {

		this.fileName = name;
		filePath = filePath.concat(name);
		file = new File(filePath);

		if (file.exists())
			System.out.println("Previous file " + name + " is deleted " + file.delete());
		else
			try {
				if (!file.createNewFile()) {
					System.out.println("Err in creating file " + name);

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void WriteIntoFile(String Line) {
		try {
			bw = new BufferedWriter(new FileWriter(filePath, true));
			bw.write(Line);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
