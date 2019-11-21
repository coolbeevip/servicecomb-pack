/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.alpha.server.fsm;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.CompensateAskFailedException;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.OmegaDisconnectedException;
import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcOmegaCallback implements OmegaCallback {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final StreamObserver<GrpcCompensateCommand> observer;
  private CompensateAsk compensateAsk;
  GrpcOmegaCallback(StreamObserver<GrpcCompensateCommand> observer) {
    this.observer = observer;
  }

  @Override
  public void compensate(TxEvent event) {
    compensateAsk = new CompensateAsk(1);
    GrpcCompensateCommand command = GrpcCompensateCommand.newBuilder()
        .setGlobalTxId(event.globalTxId())
        .setLocalTxId(event.localTxId())
        .setParentTxId(event.parentTxId() == null ? "" : event.parentTxId())
        .setCompensationMethod(event.compensationMethod())
        .setPayloads(ByteString.copyFrom(event.payloads()))
        .build();
    observer.onNext(command);
    try {
      compensateAsk.await();
      if (compensateAsk.getType() == CompensateAskType.Disconnected) {
        throw new OmegaDisconnectedException("Omega disconnected exception");
      }else{
        LOG.info("compensate ask "+compensateAsk.getType().name());
        if(compensateAsk.getType() == CompensateAskType.Failed){
          throw new CompensateAskFailedException("omega throw exception!");
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void disconnect() {
    observer.onCompleted();
    if (compensateAsk != null) {
      compensateAsk.countDown(CompensateAskType.Disconnected);
    }
  }

  public void ask(CompensateAskType type){
    compensateAsk.countDown(type);
  }
}
