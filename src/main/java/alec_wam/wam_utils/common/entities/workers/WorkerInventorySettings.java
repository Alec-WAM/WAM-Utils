package alec_wam.wam_utils.common.entities.workers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public class WorkerInventorySettings {
	
	public static final WorkerInventorySettings EMPTY_SETTINGS = new WorkerInventorySettings();
	
	public static enum IOType implements StringRepresentable {
		NONE(0, "none", false, false), IN(1, "in", true, false), OUT(2, "out", false, true), BOTH(3, "both", true, true);
		
		public static final StringRepresentable.EnumCodec<IOType> CODEC = StringRepresentable.fromEnum(IOType::values);
		private static final IntFunction<IOType> BY_ID = ByIdMap.continuous(IOType::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
		public static final StreamCodec<ByteBuf, IOType> STREAM_CODEC = ByteBufCodecs.idMapper(
			id -> {
	            IOType type = BY_ID.apply(id);
//	            System.out.println("Decoding IOType id=" + id + " => " + type);
	            return type;
	        },
	        ioType -> {
//	            System.out.println("Encoding IOType " + ioType + " => id=" + ioType.getId());
	            return ioType.getId();
	        }
		);
		
        final int id;
        final String name;
        final boolean input;
        final boolean output;
        
        IOType(int id, String name, boolean input, boolean output){
        	this.id = id;
        	this.name = name;
        	this.input = input;
        	this.output = output;
        }
        
		public IOType getNextType() {
			return IOType.values()[(this.ordinal() + 1) % IOType.values().length];
		}
		
		public boolean isInput() {
			return this.input;
		}
		
		public boolean isOutput() {
			return this.output;
		}

		public int getId() {
			return id;
		}

	    @Override
	    public String toString() {
	        return this.name;
	    }		
		
		@Override
		public String getSerializedName() {
			return this.name;
		}

        public static IOType byId(int id) {
            return BY_ID.apply(id);
        }
	}
	
	public static final Codec<WorkerInventorySettings> CODEC = RecordCodecBuilder.create(instance ->
	    instance.group(
    		GlobalPos.CODEC.optionalFieldOf("pos").forGetter((WorkerInventorySettings settings) -> {
	        	return settings.pos;
	        }),
	        Codec.unboundedMap(Direction.CODEC, IOType.CODEC).xmap(
	                EnumMap::new,  // deserialize: ImmutableMap -> HashMap
	                map -> map     // serialize: HashMap -> Map (works fine)
            ).fieldOf("ioSettings").forGetter((WorkerInventorySettings settings) -> {
	        	return (EnumMap<Direction, IOType>) settings.ioSettings;
	        })
	    ).apply(instance, WorkerInventorySettings::new)
	);
	public static final StreamCodec<ByteBuf, WorkerInventorySettings> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), (WorkerInventorySettings settings) -> {
	    	return settings.pos;
	    },
		ByteBufCodecs.map(
			(size) -> {
				return new EnumMap<>(Direction.class);
			},
			Direction.STREAM_CODEC, // codec for keys (Direction enum)
		    IOType.STREAM_CODEC                        // your enum codec for IOType values
	    ).map(
	        map -> {
	            // Convert generic Map to EnumMap
	            EnumMap<Direction, IOType> enumMap = new EnumMap<>(Direction.class);
	            enumMap.putAll(map);
	            return enumMap;
	        },
	        enumMap -> enumMap
	    ),(WorkerInventorySettings settings) -> {
	    	return settings.ioSettings;
	    },
		WorkerInventorySettings::new
	);
	
	private Optional<GlobalPos> pos;
	private EnumMap<Direction, IOType> ioSettings;
	private List<Direction> inputFaces = new ArrayList<>();
	private List<Direction> outputFaces = new ArrayList<>();
	
	public WorkerInventorySettings() {
		this(Optional.empty(), new EnumMap<>(Direction.class));
	}
	
	public WorkerInventorySettings(
			Optional<GlobalPos> pos,
			EnumMap<Direction, IOType> ioSettings
	) {
		this.pos = pos;
		this.ioSettings = ioSettings;
		this.updateFaces();
	}
	
	public GlobalPos getPos() {
		return pos.orElse(null);
	}

	public void setPos(GlobalPos pos) {
		this.pos = Optional.ofNullable(pos);
	}
	
	public IOType getIO(Direction face) {
		return this.ioSettings.getOrDefault(face, IOType.NONE);
	}
	
	public WorkerInventorySettings setIO(Direction face, IOType type) {
		EnumMap<Direction, IOType> newMap = new EnumMap<>(this.ioSettings);
	    newMap.put(face, type);
	    return new WorkerInventorySettings(this.pos, newMap);
	}
	
	private void updateFaces() {		
		this.inputFaces = new ArrayList<>();
		this.outputFaces = new ArrayList<>();
		for(Direction dir : Direction.values()) {
			IOType io = this.getIO(dir);
			if(io.isInput()) {
				this.inputFaces.add(dir);
			}
			if(io.isOutput()) {
				this.outputFaces.add(dir);
			}
		}
	}
	
	public List<Direction> getInputFaces(){
		return this.inputFaces;
	}
	
	public List<Direction> getOutputFaces(){
		return this.outputFaces;
	}
	
	public boolean hasInputFace() {
		return !this.inputFaces.isEmpty();
	}
	
	public boolean hasOutputFace() {
		return !this.outputFaces.isEmpty();
	}
	
	@Override
	public int hashCode() {
		int hash =  Objects.hash(
				this.pos,
				this.ioSettings
		);
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof WorkerInventorySettings settings) {
        	return Objects.equals(settings.pos, this.pos)
        			&& Objects.equals(settings.ioSettings, this.ioSettings);
		}
		return false;
	}
}
