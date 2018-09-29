package org.xpande.covadonga.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBOMProduct;
import org.compiere.model.MProduct;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Product: Adempiere ERP & CRM Smart Business Solution. Localization : Uruguay - Xpande
 * Xpande. Created by Gabriel Vila on 5/10/18.
 */
public class UpdateProductName extends SvrProcess {

    @Override
    protected void prepare() {

    }

    @Override
    protected String doIt() throws Exception {

        String sql = "";
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try{

            sql = " select * from aux_pp order by m_product_id ";

        	pstmt = DB.prepareStatement(sql, get_TrxName());
        	rs = pstmt.executeQuery();

        	while(rs.next()){

                MProduct product = new MProduct(getCtx(), rs.getInt("m_product_id"), get_TrxName());
                if ((product == null) || (product.get_ID() <= 0)){
                    throw new AdempiereException("No se pudo obtener producto con ID : " + rs.getInt("m_product_id"));
                }

                product.setDescription(rs.getString("name"));
                product.saveEx();
        	}
        }
        catch (Exception e){
            throw new AdempiereException(e);
        }
        finally {
            DB.close(rs, pstmt);
        	rs = null; pstmt = null;
        }

        return "OK";
    }
}
