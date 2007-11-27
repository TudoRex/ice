// **********************************************************************
//
// Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#include <Ice/RequestHandler.h>

using namespace std;
using namespace IceInternal;

IceUtil::Shared* IceInternal::upCast(RequestHandler* obj) { return obj; }

RequestHandler::~RequestHandler()
{
}

RequestHandler::RequestHandler(const ReferencePtr& reference) : _reference(reference)
{
}
