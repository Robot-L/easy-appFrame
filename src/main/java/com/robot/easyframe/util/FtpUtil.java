package com.robot.easyframe.util;

import com.asiainfo.busiframe.util.FileUtil;
import com.asiainfo.busiframe.util.PartTool;
import com.asiainfo.busiframe.util.ftp.ResFtpUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FTP相关工具
 *
 * @author luozhan
 * @date 2019-10
 */
public class FtpUtil {
    private transient static Log log = LogFactory.getLog(FtpUtil.class);

    /**
     * 上传文件到ftp远程目录
     * 上传地址为FTP_PATH_CODE配置的"REMOTE_PATH"
     *
     * @param ftpPathCode FTP_PATH_CODE （表BS_FTP_PATH）
     * @param fileContent 文件内容（回车用"\r\n"）
     * @param fileName    文件名
     * @throws Exception
     */
    public static void upload(String ftpPathCode, String fileContent, String fileName) throws Exception {
        upload(ftpPathCode, fileContent, fileName, false);
    }

    /**
     * 上传文件到ftp远程目录
     *
     * @param ftpPathCode FTP_PATH_CODE （表BS_FTP_PATH）
     * @param fileContent 文件内容（回车用\r\n）
     * @param fileName    文件名（如果isAddRecord=true，那上传的文件名将改为fileId命名，原文件名将保存在文件记录表中）
     * @param isAddRecord 是否添加FTP上传文件记录，如果为true，则在WD_F_FTPFILE表中新增记录，一般用于发送邮件附件的场景
     * @return 文件名（isAddRecord=true时返回的是fileId）
     * @throws Exception
     */
    public static String upload(String ftpPathCode, String fileContent, String fileName, boolean isAddRecord) throws Exception {
        try (InputStream fileContentStream = new ByteArrayInputStream(fileContent.getBytes());
             ResFtpUtil ftp = new ResFtpUtil(ftpPathCode)) {
            ftp.bin();
            ftp.setEncoding("UTF-8");
            int fileSize = fileContentStream.available();
            if (isAddRecord) {
                // 如果生成记录，则文件以fileId为名上传到FTP上
                fileName = generateFileId(ftp, fileName, fileSize);
                log.info(String.format("新增FTP文件记录（WD_F_FTPFILE）成功，返回fileId: %s", fileName));
            }
            ftp.upload(fileName, fileContentStream);
            log.info(String.format("上传FTP文件成功，路径：%s，文件名：%s", ftp.getRemotePath(), fileName));

            return fileName;
        }
    }

    /**
     * 生成excel文件并上传到ftp远程目录
     * （ftp上的文件名将以fileId存储，真实的文件名记录在WD_F_FTPFILE表中）
     *
     * @param ftpPathCode FTP_PATH_CODE （表BS_FTP_PATH）
     * @param data        excel数据集合，List中的每一行也是一个List，其中第一行为表头
     *                    示例：
     *                    [姓名, 年龄, 职业]
     *                    [Jay,  40,  Singer]
     *                    [Jack, 54,  Entrepreneur]
     * @param fileName    文件名（如果isAddRecord=true，那上传的文件名将改为fileId命名，原文件名将保存在文件记录表中）
     * @param isAddRecord 是否添加FTP文件记录，如果为true，则在WD_F_FTPFILE表中新增记录，一般用于发送邮件附件的场景
     * @return 文件名（isAddRecord=true时返回的是fileId）
     * @throws Exception
     * @author luozhan
     */
    public static String uploadExcel(String ftpPathCode, List<List<String>> data, String fileName, boolean isAddRecord) throws Exception {
        if (data.size() == 0) {
            throw new RuntimeException("要生成excel，数据List中至少有一个元素（表头），请检查代码");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ResFtpUtil ftp = new ResFtpUtil(ftpPathCode)) {
            // 1、生成excel文件流
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet();
            for (int i = 0; i < data.size(); i++) {
                List<String> rowData = data.get(i);
                Row row = sheet.createRow(i);
                for (int j = 0; j < rowData.size(); j++) {
                    String cellData = rowData.get(j);
                    Cell cell = row.createCell(j);
                    cell.setCellValue(cellData);
                }
            }
            // 调整每列列宽，更美观
            for (int i = 0; i < data.get(0).size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);

            // 2、上传文件
            ftp.bin();
            ftp.setEncoding("UTF-8");
            byte[] bytes = outputStream.toByteArray();
            int fileSize = bytes.length;
            if (isAddRecord) {
                // 如果生成FTP文件记录，则文件将以fileId为名上传到FTP上
                fileName = generateFileId(ftp, fileName, fileSize);
            }
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                ftp.upload(fileName, inputStream);
            }
            log.info(String.format("上传EXCEL文件成功，路径：%s，文件名：%s", ftp.getRemotePath(), fileName));

            return fileName;
        }
    }

    /**
     * 将List[Map]转换成excel需要的格式(List[List[String]])
     *
     * @param source          源数据 List[Map]
     * @param head            表头展示的元素列表
     * @param keysInSourceMap 源数据中Map取值的key，需要和表头元素列表一一对应
     * @return excelDataList
     */
    public static List<List<String>> handleExcelDataList(List<Map> source, List<String> head, List<String> keysInSourceMap) {
        if (head.size() != keysInSourceMap.size()) {
            throw new RuntimeException("表头元素个数和需要转换的List的keys个数不对应，请检查代码");
        }
        List<List<String>> excelData = source.stream().map(bean -> {
            List<String> rowData = new ArrayList<>();
            for (String key : keysInSourceMap) {
                rowData.add(PartTool.getString(bean, key, ""));
            }
            return rowData;
        }).collect(Collectors.toList());
        excelData.add(0, head);
        return excelData;
    }

    /**
     * 根据已有文件信息在WD_F_FTPFILE表中新增记录并返回fileId
     *
     * @param ftp      ftp实例
     * @param fileName 原文件名
     * @param fileSize 文件大小
     * @return fileId
     * @throws Exception
     */
    private static String generateFileId(ResFtpUtil ftp, String fileName, int fileSize) throws Exception {
        String fileId = FileUtil.makeFileId();
        FileUtil.updateFileTable(fileId, fileName, (long) fileSize, ftp.getFtpPathCode(), ftp.getRemotePath());
        log.info(String.format("新增FTP文件记录（WD_F_FTPFILE）成功，返回fileId: %s", fileId));
        return fileId;
    }
}
