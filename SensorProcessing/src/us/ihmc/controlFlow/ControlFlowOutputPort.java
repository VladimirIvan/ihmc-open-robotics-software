package us.ihmc.controlFlow;
@Deprecated
public class ControlFlowOutputPort<DataType> implements ControlFlowPort<DataType>
{
   private final String name;
   
   private DataType data;
   private final ControlFlowElement controlFlowElement;
   
   public ControlFlowOutputPort(String name, ControlFlowElement controlFlowElement)
   {
      this.name = name;
      this.controlFlowElement = controlFlowElement;
   }
   
   public String getName()
   {
      return name;
   }
   
   public DataType getData()
   {
      return data;
   }
   
   public void setData(DataType data)
   {
      this.data = data;
   }
   
   protected ControlFlowElement getControlFlowElement()
   {
      return controlFlowElement;
   }
}
