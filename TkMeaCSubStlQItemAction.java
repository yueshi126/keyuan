package epss.view.qry;

/**
 * Created with IntelliJ IDEA.
 * User: Think
 * Date: 13-2-18
 * Time: ����1:53
 * To change this template use File | Settings | File Templates.
 */
import epss.common.enums.EnumResType;
import epss.common.enums.EnumFlowStatus;
import epss.repository.model.ProgStlItemTkMea;
import skyline.util.JxlsManager;
import epss.repository.model.CttItem;
import epss.repository.model.model_show.*;
import skyline.util.MessageUtil;
import skyline.util.ToolUtil;
import epss.repository.model.CttInfo;
import epss.service.*;
import epss.service.EsQueryService;
import epss.view.flow.EsCommon;
import jxl.write.WriteException;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class TkMeaCSubStlQItemAction {
    private static final Logger logger = LoggerFactory.getLogger(TkMeaCSubStlQItemAction.class);
    @ManagedProperty(value = "#{cttInfoService}")
    private CttInfoService cttInfoService;
    @ManagedProperty(value = "#{cttItemService}")
    private CttItemService cttItemService;
    @ManagedProperty(value = "#{esCommon}")
    private EsCommon esCommon;
    @ManagedProperty(value = "#{progStlInfoService}")
    private ProgStlInfoService progStlInfoService;
    @ManagedProperty(value = "#{esQueryService}")
    private EsQueryService esQueryService;
    @ManagedProperty(value = "#{progStlItemTkMeaService}")
    private ProgStlItemTkMeaService progStlItemTkMeaService;

    /*�б���ʾ��*/
    private List<QryTkMeaCSStlQShow> qryTkMeaCSStlQShowList;
    private List<QryTkMeaCSStlQShow> qryTkMeaCSStlQShowListForExcel;

    private List<SelectItem> tkcttList;

    private String strTkcttPkid;
    private String strPeriodNo;

    // �����Ͽؼ�����ʾ����
    private ReportHeader reportHeader;
    private String strExportToExcelRendered;
    private Map beansMap;

    @PostConstruct
    public void init() {
        try {
            beansMap = new HashMap();
            reportHeader =new ReportHeader();
            // ��ȡ�Ѿ���׼�˵��ܰ���ͬ�б�
            List<CttInfoShow> cttInfoShowList =
                    cttInfoService.getCttInfoListByCttType_Status(
                            EnumResType.RES_TYPE0.getCode()
                            , EnumFlowStatus.FLOW_STATUS3.getCode());
            tkcttList=new ArrayList<SelectItem>();
            if(cttInfoShowList.size()>0){
                SelectItem selectItem=new SelectItem("","");
                tkcttList.add(selectItem);
                for(CttInfoShow itemUnit: cttInfoShowList){
                    selectItem=new SelectItem();
                    selectItem.setValue(itemUnit.getPkid());
                    selectItem.setLabel(itemUnit.getName());
                    tkcttList.add(selectItem);
                }
            }
            strPeriodNo=ToolUtil.getStrThisMonth();
        }catch (Exception e){
            logger.error("��ʼ��ʧ��", e);
        }
    }

    public String onExportExcel()throws IOException, WriteException {
        if (this.qryTkMeaCSStlQShowListForExcel.size() == 0) {
            MessageUtil.addWarn("��¼Ϊ��...");
            return null;
        } else {
            String excelFilename = "�ܰ������ɱ��ƻ��ְ����������Ƚ�-" + ToolUtil.getStrToday() + ".xls";
            JxlsManager jxls = new JxlsManager();
            jxls.exportList(excelFilename, beansMap,"qryTkMeaCSStlQ.xls");
            // ����״̬��Ʊ����Ҫ���ʱ���޸ĵ����ļ���
        }
        return null;
    }
    private void initData(String strCttInfoPkid) {
        beansMap.put("strThisMonth", ToolUtil.getStrThisMonth());
        // 1���ܰ���ͬ��Ϣ
        // 1��1��ȡ���ܰ���ͬ��Ϣ
        CttInfo esTkcttInfo= cttInfoService.getCttInfoByPkId(strCttInfoPkid);
        reportHeader.setStrTkcttId(esTkcttInfo.getId());
        reportHeader.setStrTkcttName(esTkcttInfo.getName());
        beansMap.put("reportHeader", reportHeader);
        // 1��2����ȡ��Ӧ�ܰ���ͬ����ϸ����
        List<CttItem> cttItemOfTkcttList = cttItemService.getEsItemList(
                EnumResType.RES_TYPE0.getCode(),
                strTkcttPkid);
        // �����ܰ���ͬ���ݵ���Ϣ��ƴ�ɺ�ͬԭ��
        List<CttItemShow> tkcttItemShowList =new ArrayList<>();
        recursiveDataTable("root", cttItemOfTkcttList, tkcttItemShowList);
        tkcttItemShowList =getItemList_DoFromatNo(tkcttItemShowList);

        // 2���ɱ��ƻ���Ϣ
        List<CttInfo> esCstplInfoList= cttInfoService.getEsInitCttByCttTypeAndBelongToPkId(
                EnumResType.RES_TYPE1.getCode(),esTkcttInfo.getPkid());
        if(esCstplInfoList.size()==0){
            return;
        }
        CttInfo esCstplInfo =esCstplInfoList.get(0);
        List<CttItem> cstplItemListTemp=
                cttItemService.getEsItemList(EnumResType.RES_TYPE1.getCode(),esCstplInfo.getPkid());
        List<CttItemShow> cstplItemShowListTemp =new ArrayList<>();
        recursiveDataTable("root", cstplItemListTemp, cstplItemShowListTemp);
        // �ɱ��ƻ��Ű�
        cstplItemShowListTemp =getItemList_DoFromatNo(cstplItemShowListTemp) ;

        // 3���ܰ���ͬ�����׼�˵��ܰ���������
        // С�ڵ�����ѡ���������Ѿ���׼�˵ļ�������
        String strMeaLatestApprovedPeriodNo=ToolUtil.getStrIgnoreNull(
                progStlInfoService.getLatestApprovedPeriodNoByEndPeriod(
                        EnumResType.RES_TYPE7.getCode(),strCttInfoPkid,strPeriodNo));
        List<ProgStlItemTkMea> progStlItemTkMeaList =new ArrayList<ProgStlItemTkMea>();
        if(!ToolUtil.getStrIgnoreNull(strMeaLatestApprovedPeriodNo).equals("")){
            ProgStlItemTkMea progStlItemTkMea =new ProgStlItemTkMea();
            progStlItemTkMea.setTkcttPkid(strCttInfoPkid);
            progStlItemTkMea.setPeriodNo(strMeaLatestApprovedPeriodNo);
            progStlItemTkMeaList = progStlItemTkMeaService.selectRecordsByPkidPeriodNoExample(progStlItemTkMea);
        }

        /*ƴװ�б�*/
        try {
            qryTkMeaCSStlQShowList =new ArrayList<QryTkMeaCSStlQShow>();
            QryTkMeaCSStlQShow qryTkMeaCSStlQShowForTotalAmtOfAllItem=new QryTkMeaCSStlQShow();
            BigDecimal tkcttItem_CttAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal tkcttStlItem_ThisStageAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal tkcttStlItem_AddUpAmt__TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal cstplItem_Amt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal cstplTkcttItem_TotalAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal tkcttStlCstplItem_ThisStageAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal tkcttStlCstplItem_AddUpAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal subcttStlItem_ThisStageAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal subcttStlItem_AddUpAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal meaSItem_AddUpAmt_TotalAmtOfAllItem=new BigDecimal(0);
            BigDecimal meaSCstplItem_AddUpAmt_TotalAmtOfAllItem=new BigDecimal(0);
            for(CttItemShow tkcttItemShowUnit : tkcttItemShowList){
                Boolean insertedFlag=false ;
                QryTkMeaCSStlQShow qryTkMeaCSStlQShowTemp =new QryTkMeaCSStlQShow();

                 // �ܰ���ͬ
                qryTkMeaCSStlQShowTemp.setTkcttItem_Pkid(tkcttItemShowUnit.getPkid());
                qryTkMeaCSStlQShowTemp.setTkcttItem_ParentPkid(tkcttItemShowUnit.getParentPkid());
                qryTkMeaCSStlQShowTemp.setTkcttItem_No(tkcttItemShowUnit.getStrNo());
                qryTkMeaCSStlQShowTemp.setTkcttItem_Name(tkcttItemShowUnit.getName());
                qryTkMeaCSStlQShowTemp.setTkcttItem_Unit(tkcttItemShowUnit.getUnit());
                qryTkMeaCSStlQShowTemp.setTkcttItem_CttUnitPrice(tkcttItemShowUnit.getContractUnitPrice());
                qryTkMeaCSStlQShowTemp.setTkcttItem_CttQty(tkcttItemShowUnit.getContractQuantity());
                if(tkcttItemShowUnit.getContractUnitPrice()!=null&&
                        tkcttItemShowUnit.getContractQuantity()!=null) {
                    qryTkMeaCSStlQShowTemp.setTkcttItem_CttAmt(
                            tkcttItemShowUnit.getContractUnitPrice().multiply(tkcttItemShowUnit.getContractQuantity()));
                    tkcttItem_CttAmt_TotalAmtOfAllItem=tkcttItem_CttAmt_TotalAmtOfAllItem.add(
                            ToolUtil.getBdIgnoreNull(qryTkMeaCSStlQShowTemp.getTkcttItem_CttAmt()));
                }
                // ����
                for(ProgStlItemTkMea progStlItemTkMea : progStlItemTkMeaList){
                    if(ToolUtil.getStrIgnoreNull(tkcttItemShowUnit.getPkid()).equals(progStlItemTkMea.getTkcttItemPkid())){
                        // �ܰ���ͬ����
                        BigDecimal bdTkcttContractUnitPrice=ToolUtil.getBdIgnoreNull(tkcttItemShowUnit.getContractUnitPrice());
                        BigDecimal bdTkcttStlCMeaQty=ToolUtil.getBdIgnoreNull(progStlItemTkMea.getCurrentPeriodQty());
                        BigDecimal bdTkcttStlCMeaAmount=bdTkcttStlCMeaQty.multiply(bdTkcttContractUnitPrice);
                        // ���ۼ���
                        BigDecimal bdTkcttStlBToCMeaQuantity=ToolUtil.getBdIgnoreNull(progStlItemTkMea.getBeginToCurrentPeriodQty());
                        BigDecimal bdTkcttStlBToCMeaAmount=bdTkcttStlBToCMeaQuantity.multiply(bdTkcttContractUnitPrice);
                        // ���ڼ��������ͽ��
                        qryTkMeaCSStlQShowTemp.setTkcttStlItem_ThisStageQty(bdTkcttStlCMeaQty);
                        qryTkMeaCSStlQShowTemp.setTkcttStlItem_ThisStageAmt(bdTkcttStlCMeaAmount);
                        tkcttStlItem_ThisStageAmt_TotalAmtOfAllItem=tkcttStlItem_ThisStageAmt_TotalAmtOfAllItem.add(
                                ToolUtil.getBdIgnoreNull(bdTkcttStlCMeaAmount));
                        // ���ۼ��������ͽ��
                        qryTkMeaCSStlQShowTemp.setTkcttStlItem_AddUpQty(bdTkcttStlBToCMeaQuantity);
                        qryTkMeaCSStlQShowTemp.setTkcttStlItem_AddUpAmt(bdTkcttStlBToCMeaAmount);
                        tkcttStlItem_AddUpAmt__TotalAmtOfAllItem=tkcttStlItem_AddUpAmt__TotalAmtOfAllItem.add(
                                ToolUtil.getBdIgnoreNull(bdTkcttStlBToCMeaAmount));
                        break;
                    }
                }

                // �ɱ��ƻ�
                BigDecimal bdCstplTkcttItem_TotalAmt=new BigDecimal(0);
                for(CttItemShow cstplItemShowUnit : cstplItemShowListTemp){
                    QryTkMeaCSStlQShow tkMeaCstplUnitTemp= (QryTkMeaCSStlQShow) BeanUtils.cloneBean(qryTkMeaCSStlQShowTemp);
                    if(tkcttItemShowUnit.getPkid().equals(cstplItemShowUnit.getCorrespondingPkid())) {
                        if(insertedFlag.equals(true)){
                            tkMeaCstplUnitTemp.setTkcttItem_Pkid(null);
                            tkMeaCstplUnitTemp.setTkcttItem_No(null);
                            tkMeaCstplUnitTemp.setTkcttItem_Name(null);
                            tkMeaCstplUnitTemp.setTkcttItem_Unit(null);
                            tkMeaCstplUnitTemp.setTkcttItem_CttUnitPrice(null);
                            tkMeaCstplUnitTemp.setTkcttItem_CttQty(null);
                            tkMeaCstplUnitTemp.setTkcttItem_CttAmt(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_ThisStageQty(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_ThisStageAmt(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_AddUpQty(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_AddUpAmt(null);
                        }
                        insertedFlag=true ;
                        tkMeaCstplUnitTemp.setCstplItem_Pkid(cstplItemShowUnit.getPkid());
                        tkMeaCstplUnitTemp.setCstplItem_No(cstplItemShowUnit.getStrNo());
                        tkMeaCstplUnitTemp.setCstplItem_Name(cstplItemShowUnit.getName());
                        tkMeaCstplUnitTemp.setCstplItem_UnitPrice(cstplItemShowUnit.getContractUnitPrice());
                        tkMeaCstplUnitTemp.setCstplItem_Qty(cstplItemShowUnit.getContractQuantity());
                        if(ToolUtil.getBdIgnoreNull(cstplItemShowUnit.getContractUnitPrice()).compareTo(ToolUtil.bigDecimal0)>0&&
                           ToolUtil.getBdIgnoreNull(cstplItemShowUnit.getContractQuantity()).compareTo(ToolUtil.bigDecimal0)>0) {
                            tkMeaCstplUnitTemp.setCstplItem_Amt(
                                    cstplItemShowUnit.getContractUnitPrice().multiply(cstplItemShowUnit.getContractQuantity()));
                        }
                        if(tkMeaCstplUnitTemp.getCstplItem_Amt()!=null) {
                            bdCstplTkcttItem_TotalAmt=bdCstplTkcttItem_TotalAmt.add(tkMeaCstplUnitTemp.getCstplItem_Amt()) ;
                            cstplItem_Amt_TotalAmtOfAllItem=cstplItem_Amt_TotalAmtOfAllItem.add(
                                    ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getCstplItem_Amt()));
                        }
                        qryTkMeaCSStlQShowList.add(tkMeaCstplUnitTemp);
                    }
                }

                if (insertedFlag.equals(false)){
                    qryTkMeaCSStlQShowList.add(qryTkMeaCSStlQShowTemp);
                }

                if(bdCstplTkcttItem_TotalAmt.compareTo(new BigDecimal(0))>0) {
                    qryTkMeaCSStlQShowList.get(qryTkMeaCSStlQShowList.size()-1).setCstplTkcttItem_TotalAmt(bdCstplTkcttItem_TotalAmt);
                    cstplTkcttItem_TotalAmt_TotalAmtOfAllItem=cstplTkcttItem_TotalAmt_TotalAmtOfAllItem.add(
                            ToolUtil.getBdIgnoreNull(qryTkMeaCSStlQShowList.get(qryTkMeaCSStlQShowList.size()-1).getCstplTkcttItem_TotalAmt()));
                    if(ToolUtil.getBdIgnoreNull(tkcttItemShowUnit.getContractQuantity()).compareTo(ToolUtil.bigDecimal0)>0) {
                        // ���������ֵ����
                        BigDecimal bdCstplTkcttItem_TotalUnitPrice=
                                bdCstplTkcttItem_TotalAmt.divide(tkcttItemShowUnit.getContractQuantity(),6,BigDecimal.ROUND_HALF_UP);
                        qryTkMeaCSStlQShowList.get(qryTkMeaCSStlQShowList.size()-1).setCstplTkcttItem_TotalUnitPrice(bdCstplTkcttItem_TotalUnitPrice);

                        for(int i=qryTkMeaCSStlQShowList.size()-1;i>=0;i--){
                            if(qryTkMeaCSStlQShowList.get(i).getTkcttItem_Pkid()!=null) {
                                if(qryTkMeaCSStlQShowList.get(i).getTkcttStlItem_ThisStageQty()!=null) {
                                    qryTkMeaCSStlQShowList.get(i).setTkcttStlCstplItem_ThisStageAmt(
                                            bdCstplTkcttItem_TotalUnitPrice.multiply(qryTkMeaCSStlQShowList.get(i).getTkcttStlItem_ThisStageQty()));
                                    tkcttStlCstplItem_ThisStageAmt_TotalAmtOfAllItem=tkcttStlCstplItem_ThisStageAmt_TotalAmtOfAllItem.add(
                                            ToolUtil.getBdIgnoreNull(qryTkMeaCSStlQShowList.get(i).getTkcttStlCstplItem_ThisStageAmt()));
                                }
                                if(qryTkMeaCSStlQShowList.get(i).getTkcttStlItem_AddUpQty()!=null) {
                                    qryTkMeaCSStlQShowList.get(i).setTkcttStlCstplItem_AddUpAmt(
                                            bdCstplTkcttItem_TotalUnitPrice.multiply(qryTkMeaCSStlQShowList.get(i).getTkcttStlItem_AddUpQty()));
                                    tkcttStlCstplItem_AddUpAmt_TotalAmtOfAllItem=tkcttStlCstplItem_AddUpAmt_TotalAmtOfAllItem.add(
                                            ToolUtil.getBdIgnoreNull(qryTkMeaCSStlQShowList.get(i).getTkcttStlCstplItem_AddUpAmt()));
                                }
                                break;
                            }
                        }
                    }
                }
            }
            // �ɱ��ƻ��п�ͷ����Ҫ�����һһ�г�
            for(CttItemShow cstplItemShowUnit : cstplItemShowListTemp){
                if(ToolUtil.getStrIgnoreNull(cstplItemShowUnit.getCorrespondingPkid()).length()<=0){
                    QryTkMeaCSStlQShow qryTkMeaCSStlQShowTempRe =new QryTkMeaCSStlQShow();
                    // �б�����
                    qryTkMeaCSStlQShowTempRe.setTkcttItem_Pkid(qryTkMeaCSStlQShowList.size()+":");
                    qryTkMeaCSStlQShowTempRe.setTkcttItem_Name("�ɱ��ƻ�����"+cstplItemShowUnit.getName()+")");
                    // �ɱ��ƻ�����
                    qryTkMeaCSStlQShowTempRe.setCstplItem_Pkid(cstplItemShowUnit.getPkid());
                    qryTkMeaCSStlQShowTempRe.setCstplItem_No(cstplItemShowUnit.getStrNo());
                    qryTkMeaCSStlQShowTempRe.setCstplItem_Name(cstplItemShowUnit.getName());
                    qryTkMeaCSStlQShowTempRe.setCstplItem_UnitPrice(cstplItemShowUnit.getContractUnitPrice());
                    qryTkMeaCSStlQShowTempRe.setCstplItem_Qty(cstplItemShowUnit.getContractQuantity());
                    if(!ToolUtil.getBdIgnoreNull(cstplItemShowUnit.getContractAmount()).equals(ToolUtil.bigDecimal0)) {
                        qryTkMeaCSStlQShowTempRe.setCstplItem_Amt(cstplItemShowUnit.getContractAmount());
                    } else{
                        if(cstplItemShowUnit.getContractUnitPrice()!=null&&
                                cstplItemShowUnit.getContractQuantity()!=null) {
                            qryTkMeaCSStlQShowTempRe.setCstplItem_Amt(
                                    cstplItemShowUnit.getContractUnitPrice().multiply(cstplItemShowUnit.getContractQuantity()));
                        }
                    }
                    cstplItem_Amt_TotalAmtOfAllItem=cstplItem_Amt_TotalAmtOfAllItem.add(
                            ToolUtil.getBdIgnoreNull(qryTkMeaCSStlQShowTempRe.getCstplItem_Amt()));
                    qryTkMeaCSStlQShowList.add(qryTkMeaCSStlQShowTempRe);
                }
            }

            // 4���ְ�����
            List<QryTkMeaCSStlQShow> subcttStlQBySignPartList=
                    esQueryService.getCSStlQBySignPartList(esCstplInfo.getPkid(), strPeriodNo);

            // ���ݳɱ��ƻ����ӷְ���ͬ��
            List<QryTkMeaCSStlQShow> qryTkMeaCSStlQShowListTemp=new ArrayList<>();
            qryTkMeaCSStlQShowListTemp.addAll(qryTkMeaCSStlQShowList);
            qryTkMeaCSStlQShowList.clear();
            for(QryTkMeaCSStlQShow tkMeaCstplUnit:qryTkMeaCSStlQShowListTemp) {
                Boolean insertedFlag=false ;
                BigDecimal bdSubcttCttQtyTotal=new BigDecimal(0);
                BigDecimal bdSubcttCttAmtTotal=new BigDecimal(0);
                // ���ۼ���
                BigDecimal bdTkcttStlBToCMeaQty=ToolUtil.getBdIgnoreNull(tkMeaCstplUnit.getTkcttStlItem_AddUpQty());
                BigDecimal bdTkcttStlBToCMeaAmt=ToolUtil.getBdIgnoreNull(tkMeaCstplUnit.getTkcttStlItem_AddUpAmt());
                BigDecimal bdTkcttStlCstplBToCMeaAmt=ToolUtil.getBdIgnoreNull(tkMeaCstplUnit.getTkcttStlCstplItem_AddUpAmt());
                for(int i=0;i<subcttStlQBySignPartList.size();i++) {
                    QryTkMeaCSStlQShow tkMeaCstplUnitTemp= (QryTkMeaCSStlQShow) BeanUtils.cloneBean(tkMeaCstplUnit);
                    // �ɱ��ƻ�������Ŀ��ְ���ͬ��
                    if(ToolUtil.getStrIgnoreNull(tkMeaCstplUnitTemp.getCstplItem_Pkid()).equals(
                            subcttStlQBySignPartList.get(i).getSubcttItem_CorrPkid())) {
                        // Ŀ��ְ���ͬ��ĺ�ͬ����
                        BigDecimal bdSubcttCttUnitPrice=ToolUtil.getBdIgnoreNull(subcttStlQBySignPartList.get(i).getSubcttItem_UnitPrice());
                        // Ŀ��ְ���ͬ��ĵ������������ڽ��
                        BigDecimal bdThisStageQty=ToolUtil.getBdIgnoreNull(subcttStlQBySignPartList.get(i).getSubcttStlItem_ThisStageQty());
                        BigDecimal bdThisStageAmt=bdThisStageQty.multiply(bdSubcttCttUnitPrice);
                        // Ŀ��ְ���ͬ��Ŀ������������۽��
                        BigDecimal bdAddUpToQty=ToolUtil.getBdIgnoreNull(subcttStlQBySignPartList.get(i).getSubcttStlItem_AddUpQty());
                        BigDecimal bdAddUpToAmt=bdAddUpToQty.multiply(bdSubcttCttUnitPrice);

                        // �ۼ�Ŀ��ְ���ͬ��ĺ�ͬ��������ͬ���ۣ���ͬ���
                        bdSubcttCttQtyTotal=bdSubcttCttQtyTotal.add(bdAddUpToQty);
                        bdSubcttCttAmtTotal=bdSubcttCttAmtTotal.add(bdAddUpToAmt);

                        //�ܰ���ͬ
                        if(insertedFlag.equals(true)){
                            tkMeaCstplUnitTemp.setTkcttItem_Pkid(null);
                            tkMeaCstplUnitTemp.setTkcttItem_No(null);
                            tkMeaCstplUnitTemp.setTkcttItem_Name(null);
                            tkMeaCstplUnitTemp.setTkcttItem_Unit(null);
                            tkMeaCstplUnitTemp.setTkcttItem_CttUnitPrice(null);
                            tkMeaCstplUnitTemp.setTkcttItem_CttQty(null);
                            tkMeaCstplUnitTemp.setTkcttItem_CttAmt(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_ThisStageQty(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_ThisStageAmt(null);
                            tkMeaCstplUnitTemp.setTkcttStlCstplItem_ThisStageAmt(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_AddUpQty(null);
                            tkMeaCstplUnitTemp.setTkcttStlItem_AddUpAmt(null);
                            tkMeaCstplUnitTemp.setTkcttStlCstplItem_AddUpAmt(null);
                            tkMeaCstplUnitTemp.setCstplItem_Name(null);
                            tkMeaCstplUnitTemp.setCstplItem_UnitPrice(null);
                            tkMeaCstplUnitTemp.setCstplItem_Qty(null);
                            tkMeaCstplUnitTemp.setCstplItem_Amt(null);
                            tkMeaCstplUnitTemp.setCstplTkcttItem_TotalAmt(null);
                            tkMeaCstplUnitTemp.setCstplTkcttItem_TotalUnitPrice(null);
                        }

                        insertedFlag=true ;
                        // �ְ���ͬ
                        tkMeaCstplUnitTemp.setSubcttItem_Name(subcttStlQBySignPartList.get(i).getSubcttItem_Name());
                        tkMeaCstplUnitTemp.setSubcttItem_CorrPkid(subcttStlQBySignPartList.get(i).getSubcttItem_CorrPkid());
                        tkMeaCstplUnitTemp.setSubcttItem_SignPartName(subcttStlQBySignPartList.get(i).getSubcttItem_SignPartName());

                        // �ְ�����
                        tkMeaCstplUnitTemp.setSubcttStlItem_ThisStageQty(bdThisStageQty);
                        tkMeaCstplUnitTemp.setSubcttStlItem_ThisStageAmt(bdThisStageAmt);
                        subcttStlItem_ThisStageAmt_TotalAmtOfAllItem=subcttStlItem_ThisStageAmt_TotalAmtOfAllItem.add(
                                ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getSubcttStlItem_ThisStageAmt()));
                        tkMeaCstplUnitTemp.setSubcttStlItem_AddUpQty(bdAddUpToQty);
                        tkMeaCstplUnitTemp.setSubcttStlItem_AddUpAmt(bdAddUpToAmt);
                        subcttStlItem_AddUpAmt_TotalAmtOfAllItem=subcttStlItem_AddUpAmt_TotalAmtOfAllItem.add(
                                ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getSubcttStlItem_AddUpAmt()));

                        // ���һ��֮ǰ����
                        if(i<subcttStlQBySignPartList.size()-1){
                            // ��һ������Ŀ��ְ���ͬ��
                            if(tkMeaCstplUnitTemp.getCstplItem_Pkid().equals(
                                    subcttStlQBySignPartList.get(i+1).getSubcttItem_CorrPkid())){
                                // �ɱ��ƻ����趨
                                qryTkMeaCSStlQShowList.add(tkMeaCstplUnitTemp);
                            }// ��һ���Ŀ��ְ���ͬ��
                            else{
                                tkMeaCstplUnitTemp.setMeaSItem_AddUpQty(
                                        ToolUtil.getBdIgnoreNull(bdTkcttStlBToCMeaQty).subtract(bdSubcttCttQtyTotal));
                                tkMeaCstplUnitTemp.setMeaSItem_AddUpAmt(
                                        ToolUtil.getBdIgnoreNull(bdTkcttStlBToCMeaAmt).subtract(bdSubcttCttAmtTotal));
                                tkMeaCstplUnitTemp.setMeaSCstplItem_AddUpAmt(
                                        ToolUtil.getBdIgnoreNull(bdTkcttStlCstplBToCMeaAmt).subtract(bdSubcttCttAmtTotal));
                                qryTkMeaCSStlQShowList.add(tkMeaCstplUnitTemp);
                                meaSItem_AddUpAmt_TotalAmtOfAllItem=meaSItem_AddUpAmt_TotalAmtOfAllItem.add(
                                        ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getMeaSItem_AddUpAmt()));
                                meaSCstplItem_AddUpAmt_TotalAmtOfAllItem=meaSCstplItem_AddUpAmt_TotalAmtOfAllItem.add(
                                        ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getMeaSCstplItem_AddUpAmt()));
                                break;
                            }
                        }else{
                            // �ܰ�������ְ�����ֵ��
                            tkMeaCstplUnitTemp.setMeaSItem_AddUpQty(
                                    bdTkcttStlBToCMeaQty.subtract(bdSubcttCttQtyTotal));
                            tkMeaCstplUnitTemp.setMeaSItem_AddUpAmt(
                                    bdTkcttStlBToCMeaAmt.subtract(bdSubcttCttAmtTotal));
                            tkMeaCstplUnitTemp.setMeaSCstplItem_AddUpAmt(
                                    bdTkcttStlCstplBToCMeaAmt.subtract(bdSubcttCttAmtTotal));
                            qryTkMeaCSStlQShowList.add(tkMeaCstplUnitTemp);
                            meaSItem_AddUpAmt_TotalAmtOfAllItem=meaSItem_AddUpAmt_TotalAmtOfAllItem.add(
                                    ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getMeaSItem_AddUpAmt()));
                            meaSCstplItem_AddUpAmt_TotalAmtOfAllItem=meaSCstplItem_AddUpAmt_TotalAmtOfAllItem.add(
                                    ToolUtil.getBdIgnoreNull(tkMeaCstplUnitTemp.getMeaSCstplItem_AddUpAmt()));
                        }
                    }
                }

                if(insertedFlag.equals(false)){
                    tkMeaCstplUnit.setMeaSItem_AddUpQty(tkMeaCstplUnit.getTkcttStlItem_AddUpQty());
                    tkMeaCstplUnit.setMeaSItem_AddUpAmt(tkMeaCstplUnit.getTkcttStlItem_AddUpAmt());
                    tkMeaCstplUnit.setMeaSCstplItem_AddUpAmt(tkMeaCstplUnit.getTkcttStlCstplItem_AddUpAmt());
                    qryTkMeaCSStlQShowList.add(tkMeaCstplUnit);
                    meaSItem_AddUpAmt_TotalAmtOfAllItem=meaSItem_AddUpAmt_TotalAmtOfAllItem.add(
                            ToolUtil.getBdIgnoreNull(tkMeaCstplUnit.getMeaSItem_AddUpAmt()));
                    meaSCstplItem_AddUpAmt_TotalAmtOfAllItem=meaSCstplItem_AddUpAmt_TotalAmtOfAllItem.add(
                            ToolUtil.getBdIgnoreNull(tkMeaCstplUnit.getMeaSCstplItem_AddUpAmt()));
                }
            }
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setTkcttItem_Name("�ϼ�");
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setTkcttItem_CttAmt(tkcttItem_CttAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setTkcttStlItem_ThisStageAmt(tkcttStlItem_ThisStageAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setTkcttStlItem_AddUpAmt(tkcttStlItem_AddUpAmt__TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setCstplItem_Amt(cstplItem_Amt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setCstplTkcttItem_TotalAmt(cstplTkcttItem_TotalAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setTkcttStlCstplItem_ThisStageAmt(tkcttStlCstplItem_ThisStageAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setTkcttStlCstplItem_AddUpAmt(tkcttStlCstplItem_AddUpAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setSubcttStlItem_ThisStageAmt(subcttStlItem_ThisStageAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setSubcttStlItem_AddUpAmt(subcttStlItem_AddUpAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setMeaSItem_AddUpAmt(meaSItem_AddUpAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowForTotalAmtOfAllItem.setMeaSCstplItem_AddUpAmt(meaSCstplItem_AddUpAmt_TotalAmtOfAllItem);
            qryTkMeaCSStlQShowList.add(qryTkMeaCSStlQShowForTotalAmtOfAllItem);
            // �����������װ�Excel��
            qryTkMeaCSStlQShowListForExcel =new ArrayList<QryTkMeaCSStlQShow>();
            for(QryTkMeaCSStlQShow itemOfEsItemHieRelapTkctt: qryTkMeaCSStlQShowList){
                QryTkMeaCSStlQShow itemOfEsItemHieRelapTkcttTemp=
                        (QryTkMeaCSStlQShow) BeanUtils.cloneBean(itemOfEsItemHieRelapTkctt);
                itemOfEsItemHieRelapTkcttTemp.setTkcttItem_No(
                        ToolUtil.getIgnoreSpaceOfStr(itemOfEsItemHieRelapTkcttTemp.getTkcttItem_No()));
                qryTkMeaCSStlQShowListForExcel.add(itemOfEsItemHieRelapTkcttTemp);
            }
        if(qryTkMeaCSStlQShowList.size()>0){
            strExportToExcelRendered="true";
        }else{
            strExportToExcelRendered="false";
        }
        beansMap.put("qryTkMeaCSStlQShowListForExcel", qryTkMeaCSStlQShowListForExcel);
        } catch (Exception e) {
            logger.error("��Ϣ��ѯʧ��", e);
            MessageUtil.addError("��Ϣ��ѯʧ��");
        }
    }

    /*�ݹ�����*/
    private void recursiveDataTable(String strLevelParentId,
                                      List<CttItem> cttItemListPara,
                                      List<CttItemShow> cttItemShowListPara){
        // ���ݸ��㼶�Ż�øø��㼶�µ��ӽڵ�
        // ͨ������id�������ĺ���
        List<CttItem> subCttItemList =getEsItemListByLevelParentPkid(strLevelParentId, cttItemListPara);
        for(CttItem itemUnit: subCttItemList){
            String strCreatedByName= ToolUtil.getUserName(itemUnit.getCreatedBy());
            String strLastUpdByName= ToolUtil.getUserName(itemUnit.getLastUpdBy());
            CttItemShow cttItemShowTemp = new CttItemShow(
                itemUnit.getPkid(),
                itemUnit.getBelongToType(),
                itemUnit.getBelongToPkid(),
                itemUnit.getParentPkid(),
                itemUnit.getGrade(),
                itemUnit.getOrderid(),
                itemUnit.getName(),
                itemUnit.getUnit(),
                itemUnit.getContractUnitPrice(),
                itemUnit.getContractQuantity(),
                itemUnit.getContractAmount(),
                itemUnit.getSignPartAPrice(),
                itemUnit.getArchivedFlag() ,
                itemUnit.getOriginFlag() ,
                itemUnit.getCreatedBy() ,
                strCreatedByName,
                itemUnit.getCreatedTime() ,
                itemUnit.getLastUpdBy() ,
                strLastUpdByName,
                itemUnit.getLastUpdTime() ,
                itemUnit.getRecVersion(),
                itemUnit.getRemark(),
                itemUnit.getCorrespondingPkid(),
                "",
                ""
            );
            cttItemShowListPara.add(cttItemShowTemp) ;
            recursiveDataTable(cttItemShowTemp.getPkid(), cttItemListPara, cttItemShowListPara);
        }
    }
    /*�������ݿ��в㼶��ϵ�����б�õ�ĳһ�ڵ��µ��ӽڵ�*/
    private List<CttItem> getEsItemListByLevelParentPkid(String strLevelParentPkid,
             List<CttItem> cttItemListPara) {
        List<CttItem> tempCttItemList =new ArrayList<CttItem>();
        /*�ܿ��ظ��������ݿ�*/
        for(CttItem itemUnit: cttItemListPara){
            if(strLevelParentPkid.equalsIgnoreCase(itemUnit.getParentPkid())){
                tempCttItemList.add(itemUnit);
            }
        }
        return tempCttItemList;
    }

    /*����group��orderid��ʱ���Ʊ���strNo*/
    private List<CttItemShow> getItemList_DoFromatNo(
            List<CttItemShow> cttItemShowListPara){
        String strTemp="";
        Integer intBeforeGrade=-1;
        for(CttItemShow itemUnit: cttItemShowListPara){
            if(itemUnit.getGrade().equals(intBeforeGrade)){
                if(strTemp.lastIndexOf(".")<0) {
                    strTemp=itemUnit.getOrderid().toString();
                }
                else{
                    strTemp=strTemp.substring(0,strTemp.lastIndexOf(".")) +"."+itemUnit.getOrderid().toString();
                }
            }
            else{
                if(itemUnit.getGrade()==1){
                    strTemp=itemUnit.getOrderid().toString() ;
                }
                else {
                    if (!itemUnit.getGrade().equals(intBeforeGrade)) {
                        if (itemUnit.getGrade().compareTo(intBeforeGrade)>0) {
                            strTemp = strTemp + "." + itemUnit.getOrderid().toString();
                        } else {
                            Integer intTemp=ToolUtil.lookIndex(strTemp,'.',itemUnit.getGrade()-1);
                            strTemp = strTemp .substring(0, intTemp);
                            strTemp = strTemp+"."+itemUnit.getOrderid().toString();
                        }
                    }
                }
            }
            intBeforeGrade=itemUnit.getGrade() ;
            itemUnit.setStrNo(ToolUtil.padLeft_DoLevel(itemUnit.getGrade(), strTemp)) ;
        }
        return cttItemShowListPara;
    }

    public void onQueryAction() {
        try {
            if(ToolUtil.getStrIgnoreNull(strTkcttPkid).equals("")){
                MessageUtil.addWarn("��ѡ��ɱ��ƻ��");
                return;
            }
            initData(strTkcttPkid);
            // StickyHeader��ƴװ��ͷƽ�ֿ�ȣ������趨��ȣ������Բ��ã���ʱͣ��
        } catch (Exception e) {
            logger.error("��Ϣ��ѯʧ��", e);
            MessageUtil.addError("��Ϣ��ѯʧ��");
        }
    }

    /*�����ֶ�Start*/
    public CttItemService getCttItemService() {
        return cttItemService;
    }

    public void setCttItemService(CttItemService cttItemService) {
        this.cttItemService = cttItemService;
    }

    public ProgStlInfoService getProgStlInfoService() {
        return progStlInfoService;
    }

    public void setProgStlInfoService(ProgStlInfoService progStlInfoService) {
        this.progStlInfoService = progStlInfoService;
    }

    public EsCommon getEsCommon() {
        return esCommon;
    }

    public void setEsCommon(EsCommon esCommon) {
        this.esCommon = esCommon;
    }

    public CttInfoService getCttInfoService() {
        return cttInfoService;
    }

    public void setCttInfoService(CttInfoService cttInfoService) {
        this.cttInfoService = cttInfoService;
    }

    public List<QryTkMeaCSStlQShow> getQryTkMeaCSStlQShowList() {
        return qryTkMeaCSStlQShowList;
    }

    public void setQryTkMeaCSStlQShowList(List<QryTkMeaCSStlQShow> qryTkMeaCSStlQShowList) {
        this.qryTkMeaCSStlQShowList = qryTkMeaCSStlQShowList;
    }

    public EsQueryService getEsQueryService() {
        return esQueryService;
    }

    public void setEsQueryService(EsQueryService esQueryService) {
        this.esQueryService = esQueryService;
    }

    public String getStrTkcttPkid() {
        return strTkcttPkid;
    }

    public void setStrTkcttPkid(String strTkcttPkid) {
        this.strTkcttPkid = strTkcttPkid;
    }

    public String getStrPeriodNo() {
        return strPeriodNo;
    }

    public void setStrPeriodNo(String strPeriodNo) {
        this.strPeriodNo = strPeriodNo;
    }

    public List<SelectItem> getTkcttList() {
        return tkcttList;
    }

    public void setTkcttList(List<SelectItem> tkcttList) {
        this.tkcttList = tkcttList;
    }

    public String getStrExportToExcelRendered() {
        return strExportToExcelRendered;
    }

    public void setStrExportToExcelRendered(String strExportToExcelRendered) {
        this.strExportToExcelRendered = strExportToExcelRendered;
    }

    public ReportHeader getReportHeader() {
        return reportHeader;
    }

    public void setReportHeader(ReportHeader reportHeader) {
        this.reportHeader = reportHeader;
    }

    public List<QryTkMeaCSStlQShow> getQryTkMeaCSStlQShowListForExcel() {
        return qryTkMeaCSStlQShowListForExcel;
    }

    public void setQryTkMeaCSStlQShowListForExcel(List<QryTkMeaCSStlQShow> qryTkMeaCSStlQShowListForExcel) {
        this.qryTkMeaCSStlQShowListForExcel = qryTkMeaCSStlQShowListForExcel;
    }

    public Map getBeansMap() {
        return beansMap;
    }

    public void setBeansMap(Map beansMap) {
        this.beansMap = beansMap;
    }

    public ProgStlItemTkMeaService getProgStlItemTkMeaService() {
        return progStlItemTkMeaService;
    }

    public void setProgStlItemTkMeaService(ProgStlItemTkMeaService progStlItemTkMeaService) {
        this.progStlItemTkMeaService = progStlItemTkMeaService;
    }
    /*�����ֶ�End*/
}
