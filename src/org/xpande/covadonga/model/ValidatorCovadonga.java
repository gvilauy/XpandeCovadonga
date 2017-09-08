package org.xpande.covadonga.model;

import org.compiere.acct.Doc;
import org.compiere.model.*;
import org.compiere.util.DB;
import org.xpande.comercial.model.MZComercialConfig;

import java.math.BigDecimal;

/**
 * Model Validator para Covadonga.
 * Product: Adempiere ERP & CRM Smart Business Solution. Localization : Uruguay - Xpande
 * Xpande. Created by Gabriel Vila on 9/6/17.
 */
public class ValidatorCovadonga implements ModelValidator {

    private int adClientID = 0;

    @Override
    public void initialize(ModelValidationEngine engine, MClient client) {

        // Guardo compañia
        if (client != null){
            this.adClientID = client.get_ID();
        }

        // Document Validations
        engine.addDocValidate(I_C_Invoice.Table_Name, this);
    }

    @Override
    public int getAD_Client_ID() {
        return this.adClientID;
    }

    @Override
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
        return null;
    }

    @Override
    public String modelChange(PO po, int type) throws Exception {
        return null;
    }

    @Override
    public String docValidate(PO po, int timing) {

        if (po.get_TableName().equalsIgnoreCase(I_C_Invoice.Table_Name)){
            return docValidate((MInvoice) po, timing);
        }

        return null;
    }

    /***
     * Validaciones para documentos de la tabla C_Invoice para Covadonga.
     * Xpande. Created by Gabriel Vila on 9/6/17.
     * @param model
     * @param timing
     * @return
     */
    private String docValidate(MInvoice model, int timing) {

        String message = null, sql = "", action = "";

        if ((timing == TIMING_AFTER_COMPLETE) || (timing == TIMING_AFTER_REACTIVATE)){

            // Para comprobantes de venta, guardo información para BI
            if (model.isSOTrx()){

                MDocType docType = (MDocType) model.getC_DocTypeTarget();

                // Obtengo y recorro lineas del comprobante
                MInvoiceLine[] lines = model.getLines(true);
                for (int i = 0; i < lines.length; i++){

                    MInvoiceLine invoiceLine = lines[i];

                    // Cantidad y subtotal de la linea segun documento
                    BigDecimal qtyInvoiced = invoiceLine.getQtyInvoiced();
                    BigDecimal amtSubtotal = (BigDecimal) invoiceLine.get_Value("AmtSubtotal");
                    if (amtSubtotal == null) amtSubtotal = invoiceLine.getLineNetAmt();

                    if (docType.getDocBaseType().equalsIgnoreCase(Doc.DOCTYPE_ARCredit)){
                        qtyInvoiced = qtyInvoiced.negate();
                        amtSubtotal = amtSubtotal.negate();
                    }

                    // Si estoy reactivando comprobante, doy vuelta los signos
                    if (timing == TIMING_AFTER_REACTIVATE){
                        qtyInvoiced = qtyInvoiced.negate();
                        amtSubtotal = amtSubtotal.negate();
                    }

                    // Obtengo id de dimension dia para fecha de comprobante
                    sql = " select dimday.z_bi_dia_id " +
                            "FROM c_invoice inv " +
                            " INNER JOIN z_bi_dia dimday ON cast(inv.dateinvoiced AS date) = dimday.date " +
                            " WHERE inv.c_invoice_id =" + model.get_ID();
                    int zBIDiaID = DB.getSQLValueEx(model.get_TrxName(), sql);
                    if (zBIDiaID > 0){
                        // Proceso producto de esta linea en tablas BI según ya exista o no para el dia del comprobante
                        sql = " select count(*) from z_bi_vtaproddia where m_product_id =" + invoiceLine.getM_Product_ID() +
                                " and ad_org_id =" + model.getAD_Org_ID() +
                                " and z_bi_dia_id =" + zBIDiaID;
                        int contador = DB.getSQLValueEx(model.get_TrxName(), sql);
                        if (contador > 0){
                            action = " update z_bi_vtaproddia " +
                                    " set qtyinvoiced = qtyinvoiced + " + qtyInvoiced + ", " +
                                    " amtsubtotal = amtsubtotal + " + amtSubtotal +
                                    " where m_product_id =" + invoiceLine.getM_Product_ID() +
                                    " and ad_org_id =" + model.getAD_Org_ID() +
                                    " and z_bi_dia_id =" + zBIDiaID;
                            DB.executeUpdateEx(action, model.get_TrxName());
                        }
                        else{
                            action = " insert into z_bi_vtaproddia (ad_client_id, ad_org_id, m_product_id, c_currency_id, c_uom_id, " +
                                     " z_bi_dia_id, dateinvoiced, qtyinvoiced, amtsubtotal ) " +
                                    " values (" + model.getAD_Client_ID() + ", " + model.getAD_Org_ID() + ", " + invoiceLine.getM_Product_ID() + ", " +
                                    model.getC_Currency_ID() + ", " + invoiceLine.getC_UOM_ID() + ", " + zBIDiaID + ", '" + model.getDateInvoiced() + "', " +
                                    qtyInvoiced + ", " + amtSubtotal + ") ";
                            DB.executeUpdateEx(action, model.get_TrxName());
                        }
                    }

                }

            }

        }



        return null;
    }


}
