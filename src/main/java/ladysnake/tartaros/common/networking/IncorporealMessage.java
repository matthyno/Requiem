package ladysnake.tartaros.common.networking;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class IncorporealMessage implements IMessage
  {
    long playerUUIDMost;
	long playerUUIDLeast;
    boolean simpleBool;
    
    // this constructor is required otherwise you'll get errors (used somewhere in fml through reflection)
    public IncorporealMessage() {}
    
    public IncorporealMessage(long UUIDMost, long UUIDLeast, boolean simpleBool)
    {
      this.playerUUIDMost = UUIDMost;
      this.playerUUIDLeast = UUIDLeast;
      
      this.simpleBool = simpleBool;
    }
    
    @Override
    public void fromBytes(ByteBuf buf)
    {
      // the order is important
      this.playerUUIDMost = buf.readLong();
      this.playerUUIDLeast = buf.readLong();
      this.simpleBool = buf.readBoolean();
    }
    
    @Override
    public void toBytes(ByteBuf buf)
    {
      buf.writeLong(playerUUIDMost);
      buf.writeLong(playerUUIDLeast);
      buf.writeBoolean(simpleBool);
    }
  }