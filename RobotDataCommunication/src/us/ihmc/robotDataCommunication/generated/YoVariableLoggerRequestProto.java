// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: protos/YoVariableLoggerRequest.proto

package us.ihmc.robotDataCommunication.generated;

public final class YoVariableLoggerRequestProto {
  private YoVariableLoggerRequestProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface YoVariableLoggerRequestOrBuilder
      extends com.google.protobuf.MessageOrBuilder {
    
    // required string host = 1;
    boolean hasHost();
    String getHost();
    
    // required string logName = 2;
    boolean hasLogName();
    String getLogName();
  }
  public static final class YoVariableLoggerRequest extends
      com.google.protobuf.GeneratedMessage
      implements YoVariableLoggerRequestOrBuilder {
    // Use YoVariableLoggerRequest.newBuilder() to construct.
    private YoVariableLoggerRequest(Builder builder) {
      super(builder);
    }
    private YoVariableLoggerRequest(boolean noInit) {}
    
    private static final YoVariableLoggerRequest defaultInstance;
    public static YoVariableLoggerRequest getDefaultInstance() {
      return defaultInstance;
    }
    
    public YoVariableLoggerRequest getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.internal_static_YoVariableLoggerRequest_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.internal_static_YoVariableLoggerRequest_fieldAccessorTable;
    }
    
    private int bitField0_;
    // required string host = 1;
    public static final int HOST_FIELD_NUMBER = 1;
    private java.lang.Object host_;
    public boolean hasHost() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    public String getHost() {
      java.lang.Object ref = host_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          host_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getHostBytes() {
      java.lang.Object ref = host_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        host_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    // required string logName = 2;
    public static final int LOGNAME_FIELD_NUMBER = 2;
    private java.lang.Object logName_;
    public boolean hasLogName() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    public String getLogName() {
      java.lang.Object ref = logName_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          logName_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getLogNameBytes() {
      java.lang.Object ref = logName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        logName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    private void initFields() {
      host_ = "";
      logName_ = "";
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;
      
      if (!hasHost()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLogName()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, getHostBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeBytes(2, getLogNameBytes());
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getHostBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, getLogNameBytes());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }
    
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.internal_static_YoVariableLoggerRequest_descriptor;
      }
      
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.internal_static_YoVariableLoggerRequest_fieldAccessorTable;
      }
      
      // Construct using us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }
      
      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }
      
      public Builder clear() {
        super.clear();
        host_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        logName_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest.getDescriptor();
      }
      
      public us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest getDefaultInstanceForType() {
        return us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest.getDefaultInstance();
      }
      
      public us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest build() {
        us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }
      
      private us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return result;
      }
      
      public us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest buildPartial() {
        us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest result = new us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.host_ = host_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.logName_ = logName_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest) {
          return mergeFrom((us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest other) {
        if (other == us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest.getDefaultInstance()) return this;
        if (other.hasHost()) {
          setHost(other.getHost());
        }
        if (other.hasLogName()) {
          setLogName(other.getLogName());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      public final boolean isInitialized() {
        if (!hasHost()) {
          
          return false;
        }
        if (!hasLogName()) {
          
          return false;
        }
        return true;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              onChanged();
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                onChanged();
                return this;
              }
              break;
            }
            case 10: {
              bitField0_ |= 0x00000001;
              host_ = input.readBytes();
              break;
            }
            case 18: {
              bitField0_ |= 0x00000002;
              logName_ = input.readBytes();
              break;
            }
          }
        }
      }
      
      private int bitField0_;
      
      // required string host = 1;
      private java.lang.Object host_ = "";
      public boolean hasHost() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      public String getHost() {
        java.lang.Object ref = host_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          host_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setHost(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        host_ = value;
        onChanged();
        return this;
      }
      public Builder clearHost() {
        bitField0_ = (bitField0_ & ~0x00000001);
        host_ = getDefaultInstance().getHost();
        onChanged();
        return this;
      }
      void setHost(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000001;
        host_ = value;
        onChanged();
      }
      
      // required string logName = 2;
      private java.lang.Object logName_ = "";
      public boolean hasLogName() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      public String getLogName() {
        java.lang.Object ref = logName_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          logName_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setLogName(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        logName_ = value;
        onChanged();
        return this;
      }
      public Builder clearLogName() {
        bitField0_ = (bitField0_ & ~0x00000002);
        logName_ = getDefaultInstance().getLogName();
        onChanged();
        return this;
      }
      void setLogName(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000002;
        logName_ = value;
        onChanged();
      }
      
      // @@protoc_insertion_point(builder_scope:YoVariableLoggerRequest)
    }
    
    static {
      defaultInstance = new YoVariableLoggerRequest(true);
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:YoVariableLoggerRequest)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_YoVariableLoggerRequest_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_YoVariableLoggerRequest_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n$protos/YoVariableLoggerRequest.proto\"8" +
      "\n\027YoVariableLoggerRequest\022\014\n\004host\030\001 \002(\t\022" +
      "\017\n\007logName\030\002 \002(\tBH\n(us.ihmc.robotDataCom" +
      "munication.generatedB\034YoVariableLoggerRe" +
      "questProto"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_YoVariableLoggerRequest_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_YoVariableLoggerRequest_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_YoVariableLoggerRequest_descriptor,
              new java.lang.String[] { "Host", "LogName", },
              us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest.class,
              us.ihmc.robotDataCommunication.generated.YoVariableLoggerRequestProto.YoVariableLoggerRequest.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  // @@protoc_insertion_point(outer_class_scope)
}