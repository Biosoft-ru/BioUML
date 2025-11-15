<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<dml appVersion="0.7.7" version="0.7.7">
  <diagram diagramType="biouml.plugins.wdl.diagram.WDLDiagramType" title="Hello.wdl">
    <diagramInfo title="Hello.wdl">
      <property name="Settings" short-description="Settings" type="biouml.plugins.wdl.WorkflowSettings">
        <property name="executionType" short-description="executionType" type="String" value="Nextflow"/>
        <property name="json" short-description="json" type="dataElementPath"/>
        <property name="outputPath" short-description="outputPath" type="dataElementPath"/>
        <property name="parameters" short-description="parameters" type="composite">
          <property name="person_name" short-description="person_name" type="String" value="&quot;Alice&quot;"/>
        </property>
        <property name="useJson" short-description="useJson" type="boolean" value="false"/>
      </property>
      <property name="VERSION" short-description="VERSION" type="String" value="1.0"/>
    </diagramInfo>
    <viewOptions/>
    <nodes>
      <compartment>
        <compartmentInfo height="50" isTitleHidden="false" kernel="stub/Call_ask_how_are_you" kernel_type="Call" shape="0" title="ask_how_are_you" width="200" x="392" y="200"/>
        <nodes>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/Call_ask_how_are_you/greeting_file" kernel_type="Input" title="greeting_file" width="0" x="394" y="208">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="say_hello.greeting_file"/>
            <property name="NAME" short-description="NAME" type="String" value="greeting_file"/>
            <property name="TYPE" short-description="TYPE" type="String" value="File"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/Call_ask_how_are_you/output" kernel_type="Output" title="output" width="0" x="574" y="208">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="&quot;message.txt&quot;"/>
            <property name="NAME" short-description="NAME" type="String" value="message_file"/>
            <property name="TYPE" short-description="TYPE" type="String" value="File"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
        </nodes>
        <edges/>
        <property name="CALL_NAME" short-description="CALL_NAME" type="String" value="ask_how_are_you"/>
        <property name="TASK_REFERENCE" short-description="TASK_REFERENCE" type="String" value="ask_how_are_you"/>
        <property name="innerNodesPortFinder" short-description="innerNodesPortFinder" type="boolean" value="true"/>
      </compartment>
      <compartment>
        <compartmentInfo height="50" isTitleHidden="false" kernel="stub/Call_say_hello" kernel_type="Call" shape="0" title="say_hello" width="200" x="141" y="200"/>
        <nodes>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/Call_say_hello/name" kernel_type="Input" title="name" width="0" x="143" y="208">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="person_name"/>
            <property name="NAME" short-description="NAME" type="String" value="name"/>
            <property name="TYPE" short-description="TYPE" type="String" value="String"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/Call_say_hello/output_1" kernel_type="Output" title="output_1" width="0" x="323" y="208">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="&quot;greeting.txt&quot;"/>
            <property name="NAME" short-description="NAME" type="String" value="greeting_file"/>
            <property name="TYPE" short-description="TYPE" type="String" value="File"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
        </nodes>
        <edges/>
        <property name="CALL_NAME" short-description="CALL_NAME" type="String" value="say_hello"/>
        <property name="TASK_REFERENCE" short-description="TASK_REFERENCE" type="String" value="say_hello"/>
        <property name="innerNodesPortFinder" short-description="innerNodesPortFinder" type="boolean" value="true"/>
      </compartment>
      <compartment>
        <compartmentInfo height="50" isTitleHidden="false" kernel="stub/ask_how_are_you" kernel_type="Task" shape="0" title="ask_how_are_you" width="200" x="0" y="100"/>
        <nodes>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/ask_how_are_you/input" kernel_type="Input" title="input" width="0" x="2" y="108">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String"/>
            <property name="NAME" short-description="NAME" type="String" value="greeting_file"/>
            <property name="TYPE" short-description="TYPE" type="String" value="File"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/ask_how_are_you/output" kernel_type="Output" title="output" width="0" x="182" y="108">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="&quot;message.txt&quot;"/>
            <property name="NAME" short-description="NAME" type="String" value="message_file"/>
            <property name="TYPE" short-description="TYPE" type="String" value="File"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
        </nodes>
        <edges/>
        <property elementType="biouml.plugins.wdl.Declaration" name="BEFORE_COMMAND" short-description="BEFORE_COMMAND" type="array"/>
        <property name="COMMAND" short-description="COMMAND" type="String" value="&#13;&#10;     # Read greeting, append question, write to message.txt&#13;&#10;    cat ~{greeting_file} &gt; message.txt&#13;&#10;    echo &quot; How are you?&quot; &gt;&gt; message.txt&#13;&#10;  "/>
        <property elementType="String" name="RUNTIME" short-description="RUNTIME" type="array"/>
        <property name="innerNodesPortFinder" short-description="innerNodesPortFinder" type="boolean" value="true"/>
      </compartment>
      <node height="60" isTitleHidden="false" kernel="stub/final_message" kernel_type="Workflow output" title="final_message" width="80" x="643" y="216">
        <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="ask_how_are_you.message_file"/>
        <property name="NAME" short-description="NAME" type="String" value="final_message"/>
        <property name="TYPE" short-description="TYPE" type="String" value="File"/>
      </node>
      <node height="60" isTitleHidden="false" kernel="stub/person_name" kernel_type="External_parameter" title="person_name" width="80" x="0" y="216">
        <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="&quot;Alice&quot;"/>
        <property name="NAME" short-description="NAME" type="String" value="person_name"/>
        <property name="TYPE" short-description="TYPE" type="String" value="String"/>
        <property name="position" short-description="position" type="int" value="0"/>
      </node>
      <compartment>
        <compartmentInfo height="50" isTitleHidden="false" kernel="stub/say_hello" kernel_type="Task" shape="0" title="say_hello" width="200" x="0" y="0"/>
        <nodes>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/say_hello/input_1" kernel_type="Input" title="input_1" width="0" x="2" y="8">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String"/>
            <property name="NAME" short-description="NAME" type="String" value="name"/>
            <property name="TYPE" short-description="TYPE" type="String" value="String"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
          <node fixed="true" height="0" isTitleHidden="false" kernel="stub/say_hello/output_1" kernel_type="Output" title="output_1" width="0" x="182" y="8">
            <property name="EXPRESSION" short-description="EXPRESSION" type="String" value="&quot;greeting.txt&quot;"/>
            <property name="NAME" short-description="NAME" type="String" value="greeting_file"/>
            <property name="TYPE" short-description="TYPE" type="String" value="File"/>
            <property name="position" short-description="position" type="int" value="0"/>
          </node>
        </nodes>
        <edges/>
        <property elementType="biouml.plugins.wdl.Declaration" name="BEFORE_COMMAND" short-description="BEFORE_COMMAND" type="array"/>
        <property name="COMMAND" short-description="COMMAND" type="String" value="&#13;&#10;     echo &quot;Hello, ~{name}!&quot; &gt; greeting.txt&#13;&#10;  "/>
        <property elementType="String" name="RUNTIME" short-description="RUNTIME" type="array"/>
        <property name="innerNodesPortFinder" short-description="innerNodesPortFinder" type="boolean" value="true"/>
      </compartment>
    </nodes>
    <edges>
      <edge edgeID="output_1_to_greeting_file" in="Call_say_hello/output_1" inPort="341;216" kernel="stub/output_1_to_greeting_file" kernel_type="Link" out="Call_ask_how_are_you/greeting_file" outPort="392;216" title="output_1_to_greeting_file"/>
      <edge edgeID="output_to_final_message" in="Call_ask_how_are_you/output" inPort="592;216" kernel="stub/output_to_final_message" kernel_type="Link" out="final_message" outPort="643;225" title="output_to_final_message">
        <path>
          <segment segmentType="moveTo" x0="592" y0="216"/>
          <segment segmentType="lineTo" x0="604" y0="216"/>
          <segment segmentType="cubic" x0="623" y0="216"/>
          <segment segmentType="lineTo" x0="612" y0="225"/>
          <segment segmentType="lineTo" x0="631" y0="225"/>
          <segment segmentType="lineTo" x0="643" y0="225"/>
        </path>
      </edge>
      <edge edgeID="person_name_to_name" in="person_name" inPort="90;225" kernel="stub/person_name_to_name" kernel_type="Link" out="Call_say_hello/name" outPort="141;216" title="person_name_to_name">
        <path>
          <segment segmentType="moveTo" x0="90" y0="225"/>
          <segment segmentType="lineTo" x0="102" y0="225"/>
          <segment segmentType="cubic" x0="121" y0="225"/>
          <segment segmentType="lineTo" x0="110" y0="216"/>
          <segment segmentType="lineTo" x0="129" y0="216"/>
          <segment segmentType="lineTo" x0="141" y0="216"/>
        </path>
      </edge>
    </edges>
    <filters elementType="biouml.model.DiagramFilter" type="[Lbiouml.model.DiagramFilter;"/>
  </diagram>
</dml>
