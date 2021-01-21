package com.xiebl.service.impl;

import com.xiebl.common.NormalBusiException;
import com.xiebl.mapper.UserInfoMapper;
import com.xiebl.model.UserInfo;
import com.xiebl.service.ImportExcelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
@Service
@Slf4j
@Transactional
public class ImportExcelServiceImpl implements ImportExcelService {

    @Resource
    private UserInfoMapper userInfoDao;


    @Override
    public StringBuffer uploadExcel(File temp, String fileName) throws Exception {
        Workbook wb = genWorkbook(temp, fileName);
        Sheet sheet = genSheet(wb);
        checkHead(sheet);
        List<UserInfo> userInfos = parseData(sheet);
        StringBuffer msg = saveData(userInfos);
        return msg;
    }

    /**
     * 根据文件名判断文件类型并且生成对应的工作簿类
     *
     * @param temp
     * @param fileName
     * @throws NormalBusiException
     */
    private Workbook genWorkbook(File temp, String fileName) throws NormalBusiException {
        Workbook wb = null;
        try (InputStream is = new FileInputStream(temp)) {
            if (StringUtils.endsWith(fileName, "xlsx")) {
                wb = new XSSFWorkbook(is);
            } else if (StringUtils.endsWith(fileName, "xls")) {
                wb = new HSSFWorkbook(is);
            } else {
                throw new NormalBusiException("上传失败，文件类型错误");
            }

        } catch (FileNotFoundException e) {
            throw new NormalBusiException("上传失败，上传文件没有找到", e);
        } catch (IOException e1) {
            throw new NormalBusiException("上传失败，出现IO异常，请联系管理员", e1);
        }
        return wb;
    }

    /**
     * 生成 sheet
     *
     * @param wb
     * @return
     * @throws NormalBusiException
     */
    private Sheet genSheet(Workbook wb) throws NormalBusiException {
        Sheet sheet = wb.getSheet("Sheet1");
        if (sheet == null) {
            throw new NormalBusiException("上传文件出错，sheet不存在！");
        }
        return sheet;
    }

    /**
     * 检查表头
     *
     * @param sheet
     * @throws NormalBusiException
     */
    private void checkHead(Sheet sheet) throws NormalBusiException {
        Row row = sheet.getRow(0);
        if (row == null) {
            throw new NormalBusiException("上传错误，第一行表头不能为空");
        }
        StringBuffer error = new StringBuffer();

        if (StringUtils.equals("姓名", row.getCell(0).toString())) {
            error.append("表头错误，第A列应该为姓名");
        }
        if (StringUtils.equals("年龄", row.getCell(0).toString())) {
            error.append("表头错误，第B列应该为年龄");
        }
        if (StringUtils.equals("地址", row.getCell(0).toString())) {
            error.append("表头错误，第c列应该为地址");
        }
        if (StringUtils.equals("电话", row.getCell(0).toString())) {
            error.append("表头错误，第D列应该为电话");
        }
    }

    /**
     * 解析文件具体数据内容
     *
     * @param sheet
     * @return
     * @throws NormalBusiException
     */
    private List<UserInfo> parseData(Sheet sheet) throws NormalBusiException {
        List<UserInfo> userInfos = new ArrayList<UserInfo>();
        StringBuffer info = new StringBuffer("异常！规则类型 ");
        StringBuffer error = new StringBuffer();

        int lastRowNum = sheet.getLastRowNum() + 1;
        for (int rowNum = 1; rowNum < lastRowNum; rowNum++) {
            UserInfo userInfo = new UserInfo();
            Row row = sheet.getRow(rowNum);
            for (int colNum = 0; colNum < 6; colNum++) {
                switch (colNum) {
                    case 0:
                        String userName = cellToStr(row, colNum);
                        userInfo.setUserName(userName);
                        break;
                    case 1:
                        String ageStr = cellToStr(row, colNum);
                        userInfo.setAge(Integer.parseInt(ageStr.split("\\.")[0]));
                        break;
                    case 2:
                        String address = cellToStr(row, colNum);
                        userInfo.setAddress(address);
                        break;
                    case 3:
                        String phoneNum = cellToStr(row, colNum);
                        userInfo.setPhoneNum(phoneNum.split("\\.")[0]);
                        break;
                }
            }
            userInfos.add(userInfo);
        }

        if (error.toString().length() != 0) {
            info.append(error);
            info.append(" 在系统中不存在，请先录入");
            throw new NormalBusiException(info.toString());
        }

        return userInfos;
    }

    /**
     * 保存数据
     *
     * @param userInfos
     * @return
     */
    private StringBuffer saveData(List<UserInfo> userInfos) {
        StringBuffer msg = new StringBuffer();
        for (UserInfo userInfo : userInfos) {
            userInfoDao.insertUserInfo(userInfo);
        }
        return msg;
    }

    /**
     * 把对应的cell的值拿到并且以String返回,值不能为空
     *
     * @param row
     * @param colNum
     * @return
     * @throws NormalBusiException
     */
    private String cellToStr(Row row, int colNum) throws NormalBusiException {
        Cell cell = row.getCell(colNum);
        if (cell == null || StringUtils.isBlank(cell.toString())) {
            throw new NormalBusiException("数据不能有空的");
        }
        return cell.toString();
    }


    /**
     * 获取合并单元格集合
     *
     * @param sheet
     * @return
     */
    public List<CellRangeAddress> getCombineCellList(Sheet sheet) {
        List<CellRangeAddress> list = new ArrayList<>();
        // 获得一个 sheet 中合并单元格的数量
        int sheetmergerCount = sheet.getNumMergedRegions();
        // 遍历所有的合并单元格
        for (int i = 0; i < sheetmergerCount; i++) {
            // 获得合并单元格保存进list中
            CellRangeAddress ca = sheet.getMergedRegion(i);
            list.add(ca);
        }
        return list;
    }

    /**
     * 判断cell是否为合并单元格，是的话返回合并行数和列数（只要在合并区域中的cell就会返回合同行列数，但只有左上角第一个有数据）
     *
     * @param listCombineCell 上面获取的合并区域列表
     * @param cell
     * @param sheet
     * @return
     * @throws Exception
     */
    public Map<String, Object> isCombineCell(List<CellRangeAddress> listCombineCell, Cell cell, Sheet sheet)
            throws Exception {
        int firstC = 0;
        int lastC = 0;
        int firstR = 0;
        int lastR = 0;
        String cellValue = null;
        Boolean flag = false;
        int mergedRow = 0;
        int mergedCol = 0;
        Map<String, Object> result = new HashMap<>();
        result.put("flag", flag);
        for (CellRangeAddress ca : listCombineCell) {
            // 获得合并单元格的起始行, 结束行, 起始列, 结束列
            firstC = ca.getFirstColumn();
            lastC = ca.getLastColumn();
            firstR = ca.getFirstRow();
            lastR = ca.getLastRow();
            // 判断cell是否在合并区域之内，在的话返回true和合并行列数
            if (cell.getRowIndex() >= firstR && cell.getRowIndex() <= lastR) {
                if (cell.getColumnIndex() >= firstC && cell.getColumnIndex() <= lastC) {
                    flag = true;
                    mergedRow = lastR - firstR + 1;
                    mergedCol = lastC - firstC + 1;
                    result.put("flag", true);
                    result.put("mergedRow", mergedRow);
                    result.put("mergedCol", mergedCol);
                    break;
                }
            }
        }
        return result;
    }
}
