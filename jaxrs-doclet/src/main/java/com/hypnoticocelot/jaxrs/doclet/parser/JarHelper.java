package com.hypnoticocelot.jaxrs.doclet.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarHelper {

	public void getAllFiles(File dir, List<File> fileList) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				fileList.add(file);
				if (file.isDirectory()) {
					getAllFiles(file, fileList);
				} else {
					System.out.println("file:" + file.getCanonicalPath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeZipFile(File directoryToZip, List<File> fileList,
			File outPutWarPath) {
		JarOutputStream zos = null;
		try {
			File jarFile = new File(outPutWarPath.getParentFile(),
					directoryToZip.getName() + ".war");
			FileOutputStream fos = new FileOutputStream(jarFile);
			zos = new JarOutputStream(fos);
			for (File file : fileList) {
				if (!file.isDirectory()) { // we only zip files, not directories
					addToJar(directoryToZip, file, zos);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (zos != null)
					zos.close();
			} catch (Exception ignore) {
			}
		}
	}

	private void addToJar(File directoryToZip, File file, JarOutputStream zos) throws IOException 
			{
		FileInputStream fis = null;
		String zipFilePath=null;
		try {
			// we want the zipEntry's path to be a relative path that is relative
			// to the directory being zipped, so chop off the rest of the path
			fis = new FileInputStream(file);
			zipFilePath = file.getCanonicalPath().substring(
					directoryToZip.getCanonicalPath().length() + 1,
					file.getCanonicalPath().length());
			zipFilePath = zipFilePath.replace("\\", "/");
			System.out.println("Writing '" + zipFilePath + "' to zip file");
			JarEntry zipEntry = new JarEntry(zipFilePath);
			zos.putNextEntry(zipEntry);
			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zos.write(bytes, 0, length);
			}
		} catch (FileNotFoundException fnf) {
			fnf.printStackTrace();
			throw new FileNotFoundException("FileNotFoundException occured while reading file "+file.getName());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IOException("IOException occured while adding file "+file.getName()+" as Jar entry ");
		}
		finally {
			zos.closeEntry();
			if (fis != null) {
				fis.close();
			}
		}
	}

}
