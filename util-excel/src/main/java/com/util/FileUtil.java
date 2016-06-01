package com.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts2.dispatcher.multipart.MultiPartRequestWrapper;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;

/**
 * 类功能说明 TODO:文件操作类 类修改者 修改日期 修改说明
 * <p>
 * Title: BaseService.java
 * </p>
 * <p>
 * Description:星尘科技
 * </p>
 * <p>
 * Copyright: Copyright (c) 2015
 * </p>
 * <p>
 * Company:星尘科技
 * </p>
 * 
 * @author zhusx zhushuxian12@qq.com
 * @date 2015-4-19 下午03:18:05
 * @version V1.0
 */
public class FileUtil {

	private static final Logger logger = Logger.getLogger(FileUtil.class);

	private static final int BUFFER = 1024;

	private static SftpClientUtil sftp = new SftpClientUtil();

	/**
	 * 功 能: 拷贝文件(只能拷贝文件)
	 * 
	 * @param strSourceFileName
	 *            指定的文件全路径名
	 * @param strDestDir
	 *            拷贝到指定的文件夹
	 * @return 如果成功true;否则false
	 */
	public boolean copyTo(String strSourceFileName, String strDestDir) {
		File fileSource = new File(strSourceFileName);
		File fileDest = new File(strDestDir);

		// 如果源文件不存或源文件是文件夹
		if (!fileSource.exists() || !fileSource.isFile()) {
			logger.debug("源文件[" + strSourceFileName + "],不存在或是文件夹!");
			return false;
		}

		// 如果目标文件夹不存在
		if (!fileDest.isDirectory() || !fileDest.exists()) {
			if (!fileDest.mkdirs()) {
				logger.debug("目录文件夹不存，在创建目标文件夹时失败!");
				return false;
			}
		}

		try {
			String strAbsFilename = strDestDir + File.separator
					+ fileSource.getName();

			FileInputStream fileInput = new FileInputStream(strSourceFileName);
			FileOutputStream fileOutput = new FileOutputStream(strAbsFilename);

			logger.debug("开始拷贝文件:");

			int count = -1;

			long nWriteSize = 0;
			long nFileSize = fileSource.length();

			byte[] data = new byte[BUFFER];

			while (-1 != (count = fileInput.read(data, 0, BUFFER))) {

				fileOutput.write(data, 0, count);

				nWriteSize += count;

				long size = (nWriteSize * 100) / nFileSize;
				long t = nWriteSize;

				String msg = null;

				if (size <= 100 && size >= 0) {
					msg = "\r拷贝文件进度:   " + size + "%   \t" + "\t   已拷贝:   " + t;
					logger.debug(msg);
				} else if (size > 100) {
					msg = "\r拷贝文件进度:   " + 100 + "%   \t" + "\t   已拷贝:   " + t;
					logger.debug(msg);
				}

			}

			fileInput.close();
			fileOutput.close();

			logger.debug("拷贝文件成功!");
			return true;

		} catch (Exception e) {
			logger.debug("异常信息：[");
			logger.error(e);
			logger.debug("]");
			return false;
		}
	}

	/**
	 * 删除指定的文件
	 * 
	 * @param strFileName
	 *            指定绝对路径的文件名
	 * @return 如果删除成功true否则false
	 */
	public boolean delete(String strFileName) {
		File fileDelete = new File(strFileName);

		if (!fileDelete.exists() || !fileDelete.isFile()) {
			logger.debug("错误: " + strFileName + "不存在!");
			return false;
		}

		return fileDelete.delete();
	}

	/**
	 * 移动文件(只能移动文件)
	 * 
	 * @param strSourceFileName
	 *            是指定的文件全路径名
	 * @param strDestDir
	 *            移动到指定的文件夹中
	 * @return 如果成功true; 否则false
	 */
	public boolean moveFile(String strSourceFileName, String strDestDir) {
		if (copyTo(strSourceFileName, strDestDir))
			return this.delete(strSourceFileName);
		else
			return false;
	}

	/**
	 * 创建文件夹
	 * 
	 * @param strDir
	 *            要创建的文件夹名称
	 * @return 如果成功true;否则false
	 */
	public boolean makedir(String strDir) {
		File fileNew = new File(strDir);

		if (!fileNew.exists()) {
			logger.debug("文件夹不存在--创建文件夹");
			return fileNew.mkdirs();
		} else {
			logger.debug("文件夹存在");
			return true;
		}
	}

	/**
	 * 删除文件夹
	 * 
	 * @param strDir
	 *            要删除的文件夹名称
	 * @return 如果成功true;否则false
	 */
	public boolean rmdir(String strDir) {
		File rmDir = new File(strDir);
		if (rmDir.isDirectory() && rmDir.exists()) {
			String[] fileList = rmDir.list();

			for (int i = 0; i < fileList.length; i++) {
				String subFile = strDir + File.separator + fileList[i];
				File tmp = new File(subFile);
				if (tmp.isFile())
					tmp.delete();
				else if (tmp.isDirectory())
					rmdir(subFile);
				else {
					logger.debug("error!");
				}
			}
			rmDir.delete();
		} else
			return false;
		return true;
	}

	/**
	 * 函数功能说明 Administrator修改者名字 2013-7-9修改日期 修改内容
	 * 
	 * @Title: downFile
	 * @Description: TODO:下载文件
	 * @param @param path
	 * @param @param response
	 * @param @param allPath
	 * @param @throws FileNotFoundException
	 * @param @throws IOException
	 * @param @throws UnsupportedEncodingException 设定文件
	 * @return void 返回类型
	 * @throws
	 */
	public static void downFile(String path, HttpServletResponse response,
			String allPath) throws FileNotFoundException, IOException,
			UnsupportedEncodingException {
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		OutputStream fos = null;
		InputStream fis = null;
		File uploadFile = new File(allPath);
		fis = new FileInputStream(uploadFile);
		bis = new BufferedInputStream(fis);
		fos = response.getOutputStream();
		bos = new BufferedOutputStream(fos);
		response.setHeader("Content-disposition", "attachment;filename="
				+ URLEncoder.encode(path, "utf-8"));
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
		while ((bytesRead = bis.read(buffer, 0, 8192)) != -1) {
			bos.write(buffer, 0, bytesRead);
		}
		bos.flush();
		fis.close();
		bis.close();
		fos.close();
		bos.close();
	}
	/**
	 * 上传文件 默认文件名为uploadify
	 * @param request
	 * @param map
	 * @return
	 */
	public static String uploadFile(HttpServletRequest request,Map<String, Object> map) {
		String url = sftp.url;
		try {
			MultiPartRequestWrapper mpRequest = (MultiPartRequestWrapper) request;
			File[] files = mpRequest.getFiles("uploadify"); // 文件现在还在临时目录中
			String[] fileNames = mpRequest.getFileNames("uploadify");
			if (files.length>0&&fileNames.length>0) {
				Long fileSize = Long.valueOf(request.getHeader("Content-Length"));// 上传的文件大小
				String fileName = fileNames[0];
				ServletInputStream inputStream = null;
				try {
					inputStream = request.getInputStream();
				} catch (IOException e) {
					map.put("err", "上传文件出错！");
				}

				if (inputStream == null) {
					map.put("err", "您没有上传任何文件！");
				}

				if (fileSize > ResourceUtil.getUploadFileMaxSize()) {
					map.put("err", "上传文件超出限制大小！");
					map.put("msg", fileName);
				} else {
					// 检查文件扩展名
					String fileExt = fileName.substring(
							fileName.lastIndexOf(".") + 1).toLowerCase();
					String newFileName = UUID.randomUUID().toString()
							.replaceAll("-", "")
							+ "." + fileExt;// 新的文件名称
					boolean flag = sftp.upload(new FileInputStream(files[0]),
							newFileName);
					url += newFileName;
					if (flag) {
						map.put("err", "上传文件成功！");
					} else {
						map.put("err", "上传文件出错！");
					}
					map.put("err", "");
					logger.info("----" + url);
					Map<String, Object> nm = new HashMap<String, Object>();
					nm.put("url", url);
					nm.put("localfile", fileName);
					nm.put("id", 0);
					map.put("msg", nm);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return url;
	}
	
	public static String uploadFileByName(HttpServletRequest request,String name,Map<String, Object> map) {
		String url = null;
		try {
			MultiPartRequestWrapper mpRequest = (MultiPartRequestWrapper) request;
			File[] files = mpRequest.getFiles(name); // 文件现在还在临时目录中
			String[] fileNames = mpRequest.getFileNames(name);
			if (files.length>0&&fileNames.length>0) {
				String contentlength  =  request.getHeader("Content-Length");
				if (contentlength==null) {
					contentlength="0";
				}
				Long fileSize = Long.valueOf(contentlength);// 上传的文件大小
				String fileName = fileNames[0];
				ServletInputStream inputStream = null;
				try {
					inputStream = request.getInputStream();
				} catch (IOException e) {
					map.put("err", "上传文件出错！");
				}

				if (inputStream == null) {
					map.put("err", "您没有上传任何文件！");
				}

				if (fileSize > ResourceUtil.getUploadFileMaxSize()) {
					map.put("err", "上传文件超出限制大小！");
					map.put("msg", fileName);
				} else {
					// 检查文件扩展名
					String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					String newFileName = UUID.randomUUID().toString().replaceAll("-", "")+ "." + fileExt;// 新的文件名称
					boolean flag = sftp.upload(new FileInputStream(files[0]),newFileName);
					url = sftp.url+newFileName;
					if (flag) {
						map.put("err", "上传文件成功！");
					} else {
						map.put("err", "上传文件出错！");
					}
					map.put("err", "");
					logger.info("----" + url);
					map.put("url", url);
					map.put("msg", fileName);
				}
			}
		} catch (Exception e) {
			map.put("err", "上传文件出错！");
			e.printStackTrace();
		}
		return url;
	}
}