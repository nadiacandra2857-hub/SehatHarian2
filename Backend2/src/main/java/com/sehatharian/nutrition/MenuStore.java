package com.sehatharian.nutrition;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.*;
import com.sehatharian.storage.WriteTransaction;
import java.sql.*;

/** Owner -> CMS -> food rows is the shared lock order for coherent recommendations and meal commits. */
public abstract class MenuStore extends OwnedRepository {
    protected MenuStore(SessionAccess access){super(access);}
    protected MenuStore(SessionAccess access,PenggunaRepository.ConnectionFactory connections){super(access,connections);}
    @FunctionalInterface protected interface Work<T>{T run(Connection c,WriteTransaction tx)throws SQLException;}
    protected <T>T transaction(boolean personal,Work<T> work)throws SQLException {
        var tx=new WriteTransaction(access,connections);
        try{var c=tx.open();if(personal)lockOwner(c,access);lockCatalog(c,access);return tx.confirm(work.run(c,tx));}
        catch(SQLException e){return tx.recover(e);}catch(RuntimeException e){tx.abort();throw e;}finally{tx.close();}
    }
    public static void lockOwner(Connection c,SessionAccess access)throws SQLException{
        try(var p=c.prepareStatement("SELECT id FROM pengguna WHERE id=? FOR UPDATE")){p.setInt(1,access.owner());try(var r=p.executeQuery()){if(!r.next())throw new SecurityException("Pemilik tidak tersedia.");}}
    }
    public static void lockCatalog(Connection c,SessionAccess access)throws SQLException{
        access.owner();try(var p=c.prepareStatement("SELECT id FROM content_guard WHERE id=1 FOR UPDATE");var r=p.executeQuery()){if(!r.next())throw new SQLException("Kunci katalog belum tersedia.");}
    }
}
