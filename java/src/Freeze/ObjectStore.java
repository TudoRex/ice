// **********************************************************************
//
// Copyright (c) 2004
// ZeroC, Inc.
// Billerica, MA, USA
//
// All Rights Reserved.
//
// Ice is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License version 2 as published by
// the Free Software Foundation.
//
// **********************************************************************

package Freeze;

class ObjectStore implements IceUtil.Store
{
    
    ObjectStore(String facet, boolean createDb, EvictorI evictor, 
		java.util.List indices, boolean populateEmptyIndices)
    {
	_cache = new IceUtil.Cache(this);
	
	_facet = facet;
	_evictor = evictor;
	_indices = indices;
	_communicator = evictor.communicator();
	
	if(facet.equals(""))
	{
	    _dbName = EvictorI.defaultDb;
	}
	else
	{
	    _dbName = facet;
	}

	com.sleepycat.db.DbTxn txn = null;
	com.sleepycat.db.DbEnv dbEnv = evictor.dbEnv();

	try
	{	    
	    _db = new com.sleepycat.db.Db(dbEnv, 0);
	    
	    txn = dbEnv.txn_begin(null, 0);

	    //
	    // TODO: FREEZE_DB_MODE
	    //
	    int flags = 0;
	    if(createDb)
	    {
		flags |= com.sleepycat.db.Db.DB_CREATE;
	    }
	    _db.open(txn, evictor.filename(), _dbName, com.sleepycat.db.Db.DB_BTREE, flags, 0);

	   
	    java.util.Iterator p = _indices.iterator();
	    while(p.hasNext())
	    {
		Index index = (Index) p.next();
		index.associate(this, txn, createDb, populateEmptyIndices);
	    }

	    com.sleepycat.db.DbTxn toCommit = txn;
	    txn = null;
	    toCommit.commit(0);
	}
	catch(java.io.FileNotFoundException dx)
	{
	    NotFoundException ex = new NotFoundException();
	    ex.initCause(dx);
	    ex.message = _evictor.errorPrefix() + "Db.open: " + dx.getMessage();
	    throw ex;
	}
	catch(com.sleepycat.db.DbException dx)
	{
	    DatabaseException ex = new DatabaseException();
	    ex.initCause(dx);
	    ex.message = _evictor.errorPrefix() + "Db.open: " + dx.getMessage();
	    throw ex;
	}
	finally
	{
	    if(txn != null)
	    {
		try
		{
		    txn.abort();
		}
		catch(com.sleepycat.db.DbException dx)
		{
		}
	    }
	}
    }

    protected void
    finalize()
    {
        if(_db != null)
        {
            close();
        }
    }
    
    void
    close()
    {
	try
	{
	    _db.close(0);
	   
	    java.util.Iterator p = _indices.iterator();
	    while(p.hasNext())
	    {
		Index index = (Index) p.next();
		index.close();
	    }
	    _indices.clear();
	}
	catch(com.sleepycat.db.DbException dx)
	{
	    DatabaseException ex = new DatabaseException();
	    ex.initCause(dx);
	    ex.message = _evictor.errorPrefix() + "Db.close: " + dx.getMessage();
	    throw ex;
	}
	_db = null;
    }
    
    boolean
    dbHasObject(Ice.Identity ident)
    {
	byte[] key = marshalKey(ident, _communicator);
	com.sleepycat.db.Dbt dbKey = new com.sleepycat.db.Dbt(key);
		
	//
	// Keep 0 length since we're not interested in the data
	//
	com.sleepycat.db.Dbt dbValue = new com.sleepycat.db.Dbt();
	dbValue.set_flags(com.sleepycat.db.Db.DB_DBT_PARTIAL);
	
	for(;;)
	{
	    try
	    {	
		int err = _db.get(null, dbKey, dbValue, 0);
		
		if(err == 0)
		{
		    return true;
		}
		else if(err == com.sleepycat.db.Db.DB_NOTFOUND)
		{
		    return false;
		}
		else
		{
		    throw new DatabaseException();
		}
	    }
	    catch(com.sleepycat.db.DbDeadlockException deadlock)
	    {
		if(_evictor.deadlockWarning())
		{
		    _communicator.getLogger().warning
			("Deadlock in Freeze.ObjectStore.dhHasObject while reading Db \"" 
			 + _evictor.filename() + "/" + _dbName 
			 + "\"; retrying ...");
		}

		//
		// Ignored, try again
		//
	    }
	    catch(com.sleepycat.db.DbException dx)
	    {
		DatabaseException ex = new DatabaseException();
		ex.initCause(dx);
		ex.message = _evictor.errorPrefix() + "Db.get: " + dx.getMessage();
		throw ex;
	    }
	}
    }

    void
    save(byte[] key, byte[] value, byte status, com.sleepycat.db.DbTxn tx)
	throws com.sleepycat.db.DbException
    {
	switch(status)
	{
	    case EvictorElement.created:
	    case EvictorElement.modified:
	    {
		com.sleepycat.db.Dbt dbKey = new com.sleepycat.db.Dbt(key);
		com.sleepycat.db.Dbt dbValue = new com.sleepycat.db.Dbt(value);
		int flags = (status == EvictorElement.created) ? com.sleepycat.db.Db.DB_NOOVERWRITE : 0;
		int err = _db.put(tx, dbKey, dbValue, flags);
		if(err != 0)
		{
		    throw new DatabaseException();
		}
		break;
	    }
	    case EvictorElement.destroyed:
	    {
		com.sleepycat.db.Dbt dbKey = new com.sleepycat.db.Dbt(key);
		int err = _db.del(tx, dbKey, 0);
		if(err != 0)
		{
		    throw new DatabaseException();
		}
		break;
	    }
	    default:
	    {
		assert false;
	    }
	}
    }

    static byte[]
    marshalKey(Ice.Identity v, Ice.Communicator communicator)
    {
        IceInternal.BasicStream os = new IceInternal.BasicStream(Ice.Util.getInstance(communicator));
        try
        {
            v.__write(os);
            java.nio.ByteBuffer buf = os.prepareWrite();
            byte[] r = new byte[buf.limit()];
            buf.get(r);
            return r;
        }
        finally
        {
            os.destroy();
        }
    }

    static Ice.Identity
    unmarshalKey(byte[] b, Ice.Communicator communicator)
    {
        IceInternal.BasicStream is = new IceInternal.BasicStream(Ice.Util.getInstance(communicator));
        try
        {
            is.resize(b.length, true);
            java.nio.ByteBuffer buf = is.prepareRead();
            buf.position(0);
            buf.put(b);
            buf.position(0);
            Ice.Identity key = new Ice.Identity();
            key.__read(is);
            return key;
        }
        finally
        {
            is.destroy();
        }
    }

    static byte[]
    marshalValue(ObjectRecord v, Ice.Communicator communicator)
    {
        IceInternal.BasicStream os = new IceInternal.BasicStream(Ice.Util.getInstance(communicator));
        try
        {
            os.startWriteEncaps();
            v.__write(os);
            os.writePendingObjects();
            os.endWriteEncaps();
            java.nio.ByteBuffer buf = os.prepareWrite();
            byte[] r = new byte[buf.limit()];
            buf.get(r);
            return r;
        }
        finally
        {
            os.destroy();
        }
    }

    static ObjectRecord
    unmarshalValue(byte[] b, Ice.Communicator communicator)
    {
        IceInternal.BasicStream is = new IceInternal.BasicStream(Ice.Util.getInstance(communicator));
        is.sliceObjects(false);
        try
        {
            is.resize(b.length, true);
            java.nio.ByteBuffer buf = is.prepareRead();
            buf.position(0);
            buf.put(b);
            buf.position(0);
            ObjectRecord rec= new ObjectRecord();
            is.startReadEncaps();
            rec.__read(is);
            is.readPendingObjects();
            is.endReadEncaps();
            return rec;
        }
        finally
        {
            is.destroy();
        }
    }


    final IceUtil.Cache
    cache()
    {
	return _cache;
    }

    final com.sleepycat.db.Db 
    db()
    {
	return _db;
    }
    
    final Ice.Communicator
    communicator()
    {
	return _communicator;
    }
    
    final EvictorI
    evictor()
    {
	return _evictor;
    }
    
    final String
    facet()
    {
	return _facet;
    }

    final String
    dbName()
    {
	return _dbName;
    }

    public Object
    load(Object identObj)
    {
	Ice.Identity ident = (Ice.Identity) identObj;

	byte[] key = marshalKey(ident, _communicator);

	com.sleepycat.db.Dbt dbKey = new com.sleepycat.db.Dbt(key);
	com.sleepycat.db.Dbt dbValue = new com.sleepycat.db.Dbt();

	for(;;)
	{
	    try
	    {	
		int rs = _db.get(null, dbKey, dbValue, 0);
		
		if(rs == com.sleepycat.db.Db.DB_NOTFOUND)
		{
		    return null;
		}
		else if (rs != 0)
		{
		    assert false;
		    throw new DatabaseException();
		}
		break;
	    }
	    catch(com.sleepycat.db.DbDeadlockException deadlock)
	    {
		if(_evictor.deadlockWarning())
		{
		    _communicator.getLogger().warning
			("Deadlock in Freeze.ObjectStore.load while reading Db \"" 
			 + _evictor.filename() + "/" + _dbName 
			 + "\"; retrying ...");
		}

		//
		// Ignored, try again
		//
	    }
	    catch(com.sleepycat.db.DbException dx)
	    {
		DatabaseException ex = new DatabaseException();
		ex.initCause(dx);
		ex.message = _evictor.errorPrefix() + "Db.get: " + dx.getMessage();
		throw ex;
	    }
	}
	
	EvictorElement result = new EvictorElement(ident, this);
	result.rec = unmarshalValue(dbValue.get_data(), _communicator);
	
	_evictor.initialize(ident, _facet, result.rec.servant);
	return result;
    }

    private final IceUtil.Cache _cache;
    
    private com.sleepycat.db.Db _db;
    private final String _facet;
    private final String _dbName;
    private final EvictorI _evictor;
    private final java.util.List _indices;
    private final Ice.Communicator _communicator;

}







