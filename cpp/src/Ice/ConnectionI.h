// **********************************************************************
//
// Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#ifndef ICE_CONNECTION_I_H
#define ICE_CONNECTION_I_H

#include <IceUtil/Mutex.h>
#include <IceUtil/Monitor.h>
#include <IceUtil/Time.h>
#include <IceUtil/Thread.h> // For ThreadPerConnection.
#include <Ice/Connection.h>
#include <Ice/ConnectionIF.h>
#include <Ice/ConnectionFactoryF.h>
#include <Ice/InstanceF.h>
#include <Ice/TransceiverF.h>
#include <Ice/ObjectAdapterF.h>
#include <Ice/ServantManagerF.h>
#include <Ice/EndpointIF.h>
#include <Ice/LoggerF.h>
#include <Ice/TraceLevelsF.h>
#include <Ice/OutgoingAsyncF.h>
#include <Ice/EventHandler.h>
#include <Ice/SelectorThread.h>

#include <deque>

namespace IceInternal
{

class Outgoing;
class BatchOutgoing;
class OutgoingMessageCallback;

}

namespace Ice
{

class LocalException;

class ICE_API ConnectionI : public Connection, 
                            public IceInternal::EventHandler,
                            public IceInternal::SocketReadyCallback,
                            public IceUtil::Monitor<IceUtil::Mutex>
{
public:

    class StartCallback : virtual public IceUtil::Shared
    {
    public:
        
        virtual void connectionStartCompleted(const ConnectionIPtr&) = 0;
        virtual void connectionStartFailed(const ConnectionIPtr&, const Ice::LocalException&) = 0;
    };
    typedef IceUtil::Handle<StartCallback> StartCallbackPtr;

    enum DestructionReason
    {
        ObjectAdapterDeactivated,
        CommunicatorDestroyed
    };

    void start(const StartCallbackPtr&);
    void activate();
    void hold();
    void destroy(DestructionReason);
    virtual void close(bool); // From Connection.

    bool isActiveOrHolding() const;
    bool isFinished() const;

    void throwException() const; // Throws the connection exception if destroyed.

    void waitUntilHolding() const;
    void waitUntilFinished(); // Not const, as this might close the connection upon timeout.

    void monitor();

    bool sendRequest(IceInternal::Outgoing*, bool, bool);
    void sendAsyncRequest(const IceInternal::OutgoingAsyncPtr&, bool, bool);

    void prepareBatchRequest(IceInternal::BasicStream*);
    void finishBatchRequest(IceInternal::BasicStream*, bool);
    void abortBatchRequest();

    virtual void flushBatchRequests(); // From Connection.

    bool flushBatchRequests(IceInternal::BatchOutgoing*);
    void flushAsyncBatchRequests(const IceInternal::BatchOutgoingAsyncPtr&);

    void sendResponse(IceInternal::BasicStream*, Byte);
    void sendNoResponse();

    IceInternal::EndpointIPtr endpoint() const;
    bool threadPerConnection() const;

    virtual void setAdapter(const ObjectAdapterPtr&); // From Connection.
    virtual ObjectAdapterPtr getAdapter() const; // From Connection.
    virtual ObjectPrx createProxy(const Identity& ident) const; // From Connection.

    //
    // Operations from EventHandler
    //
    virtual bool datagram() const;
    virtual bool readable() const;
    virtual bool read(IceInternal::BasicStream&);
    virtual void message(IceInternal::BasicStream&, const IceInternal::ThreadPoolPtr&);
    virtual void finished(const IceInternal::ThreadPoolPtr&);
    virtual void exception(const LocalException&);
    virtual void invokeException(const LocalException&, int);
    virtual std::string type() const; // From Connection.
    virtual Ice::Int timeout() const; // From Connection.
    virtual std::string toString() const;  // From Connection and EvantHandler.

    //
    // Operations from SocketReadyCallback
    //
    virtual IceInternal::SocketStatus socketReady(bool);
    virtual void socketTimeout();

    // SSL plug-in needs to be able to get the transceiver.
    IceInternal::TransceiverPtr getTransceiver() const;

private:

    ConnectionI(const IceInternal::InstancePtr&, const IceInternal::TransceiverPtr&, 
                const IceInternal::EndpointIPtr&, const ObjectAdapterPtr&, bool, size_t);
    virtual ~ConnectionI();

    friend class IceInternal::IncomingConnectionFactory;
    friend class IceInternal::OutgoingConnectionFactory;

    enum State
    {
        StateNotInitialized,
        StateNotValidated,
        StateActive,
        StateHolding,
        StateClosing,
        StateClosed
    };

    bool setState(State, const LocalException&);
    bool setState(State);

    bool initiateShutdown(bool);

    struct OutgoingMessage
    {
        OutgoingMessage()
	{
	}

        OutgoingMessage(IceInternal::BasicStream* str, bool comp) : 
	    stream(str), out(0), compress(comp), response(false), adopted(false)
	{
	}

        OutgoingMessage(IceInternal::OutgoingMessageCallback* o, IceInternal::BasicStream* str, bool comp, bool resp) :
	    stream(str), out(o), compress(comp), response(resp), adopted(false)
	{
	}

        OutgoingMessage(const IceInternal::OutgoingAsyncMessageCallbackPtr& o, IceInternal::BasicStream* str, 
                        bool comp, bool resp) :
	    stream(str), out(0), outAsync(o), compress(comp), response(resp), adopted(false)
	{
	}

        void adopt(IceInternal::BasicStream*);
        void sent(ConnectionI*, bool);
        void finished(const Ice::LocalException&);

        IceInternal::BasicStream* stream;
        IceInternal::OutgoingMessageCallback* out;
        IceInternal::OutgoingAsyncMessageCallbackPtr outAsync;
        bool compress;
        bool response;
        bool adopted;
    };

    IceInternal::SocketStatus initialize();
    IceInternal::SocketStatus validate();
    bool send(int);
    bool sendMessage(OutgoingMessage&, bool = false);
    void finishSendMessage();

    void finishStart();
    void finishStart(const Ice::LocalException&);

    void registerWithPool();
    void unregisterWithPool();

    void doCompress(IceInternal::BasicStream&, IceInternal::BasicStream&);
    void doUncompress(IceInternal::BasicStream&, IceInternal::BasicStream&);

    void parseMessage(IceInternal::BasicStream&, Int&, Int&, Byte&,
                      IceInternal::ServantManagerPtr&, ObjectAdapterPtr&, IceInternal::OutgoingAsyncPtr&);
    void invokeAll(IceInternal::BasicStream&, Int, Int, Byte,
                   const IceInternal::ServantManagerPtr&, const ObjectAdapterPtr&);

    void run(); // For thread per connection.

    class ThreadPerConnection : public IceUtil::Thread
    {
    public:
        
        ThreadPerConnection(const ConnectionIPtr&);
        virtual void run();

    private:
        
        ConnectionIPtr _connection;
    };
    friend class ThreadPerConnection;
    // Defined as mutable because "isFinished() const" sets this to 0.
    mutable IceUtil::ThreadPtr _thread;
    const bool _threadPerConnection;
    const size_t _threadPerConnectionStackSize;

    IceInternal::TransceiverPtr _transceiver;
    const std::string _desc;
    const std::string _type;
    const IceInternal::EndpointIPtr _endpoint;

    ObjectAdapterPtr _adapter;
    IceInternal::ServantManagerPtr _servantManager;

    const LoggerPtr _logger;
    const IceInternal::TraceLevelsPtr _traceLevels;

    bool _registeredWithPool;
    int _finishedCount;
    const IceInternal::ThreadPoolPtr _threadPool;

    const IceInternal::SelectorThreadPtr _selectorThread;

    StartCallbackPtr _startCallback;

    const bool _warn;

    const int _acmTimeout;
    IceUtil::Time _acmAbsoluteTimeout;

    const int _compressionLevel;

    Int _nextRequestId;

    std::map<Int, IceInternal::Outgoing*> _requests;
    std::map<Int, IceInternal::Outgoing*>::iterator _requestsHint;

    std::map<Int, IceInternal::OutgoingAsyncPtr> _asyncRequests;
    std::map<Int, IceInternal::OutgoingAsyncPtr>::iterator _asyncRequestsHint;

    std::auto_ptr<LocalException> _exception;

    const bool _batchAutoFlush;
    IceInternal::BasicStream _batchStream;
    bool _batchStreamInUse;
    int _batchRequestNum;
    bool _batchRequestCompress;
    size_t _batchMarker;

    std::deque<OutgoingMessage> _queuedStreams;
    std::deque<OutgoingMessage> _sendStreams;
    bool _sendInProgress;
    int _waitingForSend;
    
    int _dispatchCount;

    State _state; // The current state.
    IceUtil::Time _stateTime; // The last time when the state was changed.
};

}

#endif
