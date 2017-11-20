/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.kernel.comm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twosigma.beakerx.CodeCell;
import com.twosigma.beakerx.NamespaceClient;
import com.twosigma.beakerx.jvm.serialization.BasicObjectSerializer;
import com.twosigma.beakerx.jvm.serialization.BeakerObjectConverter;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.message.Message;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static com.twosigma.beakerx.handler.KernelHandlerWrapper.wrapBusyIdle;


public class GetCodeCellsHandler extends BaseHandler<Object> {

  private static final String GET_CODE_CELLS = "code_cells";
  private ObjectMapper objectMapper;
  private BeakerObjectConverter objectSerializer;

  public GetCodeCellsHandler(KernelFunctionality kernel) {
    super(kernel);
    objectMapper = new ObjectMapper();
    objectSerializer = new BasicObjectSerializer();
  }

  @Override
  public void handle(Message message) {
    wrapBusyIdle(kernel, message, () -> {
      handleMsg(message);
    });
  }

  private void handleMsg(Message message) {
    try {
      List<CodeCell> cells = getBeakerCodeCells(getValueFromData(message, getHandlerCommand()));
      NamespaceClient.getMessageQueue("CodeCells").put(cells);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private List<CodeCell> getBeakerCodeCells(Object value) {
    List<CodeCell> codeCellList = null;
    StringWriter sw = new StringWriter();
    JsonGenerator jgen = null;
    try {
      jgen = objectMapper.getFactory().createGenerator(sw);
      objectSerializer.writeObject(value, jgen, true);
      jgen.flush();
      sw.flush();
      codeCellList = Arrays.asList(objectMapper.readValue(sw.toString(), CodeCell[].class));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return codeCellList;
  }

  @Override
  public String getHandlerCommand() {
    return GET_CODE_CELLS;
  }
}
