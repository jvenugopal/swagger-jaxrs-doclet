package com.hypnoticocelot.jaxrs.doclet.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarHelper {

	public void getAllFiles(File dir, List<File> fileList) throws IOException {
		File[] files = dir.listFiles();
		for (File file : files) {
			fileList.add(file);
			if (file.isDirectory()) {
				getAllFiles(file, fileList);
			} else {
				System.out.println("file:" + file.getCanonicalPath());
			}
		}
	}

	public void writeZipFile(File directoryToZip, List<File> fileList, File outPutWarPath) throws IOException {
		FileOutputStream fos = null;
		JarOutputStream zos = null;
		try {
			File jarFile = new File(outPutWarPath.getParentFile(), directoryToZip.getName() + ".war");
			fos = new FileOutputStream(jarFile);
			zos = new JarOutputStream(fos);
			for (File file : fileList) {
				if (!file.isDirectory()) { // we only zip files, not directories
					addToJar(directoryToZip, file, zos);
				}
			}
			zos.finish();
		} finally {
			closeSilently(fos);
			closeSilently(zos);
		}
	}

	private void addToJar(File directoryToZip, File file, JarOutputStream zos)
			throws IOException {
		FileInputStream fis = null;
		String zipFilePath = null;
		try {
			// we want the zipEntry's path to be a relative path that is
			// relative
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
			zos.flush();
			zos.closeEntry();
		} finally {
			closeSilently(fis);
		}
	}

	private void closeSilently(InputStream fis) {
		if (fis != null) {
			try {
				fis.close();
			} catch (Exception ignore) {
			}
		}
	}
	
	private void closeSilently(OutputStream fos) {
		if (fos != null) {
			try {
				fos.close();
			} catch (Exception ignore) {
			}
		}
	}
	

}
