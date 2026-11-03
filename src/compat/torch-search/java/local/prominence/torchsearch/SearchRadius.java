package local.prominence.torchsearch;
import fuzs.magnumtorch.config.SpawnShapeType;
import fuzs.magnumtorch.config.ServerConfig;
public final class SearchRadius {
 private SearchRadius(){}
 public static int forConfig(int original,ServerConfig.MagnumTorchConfig config){
  return config==null?original:tightened(original,config.shapeType,config.horizontalRange,config.verticalRange);
 }
 public static int tightened(int original,SpawnShapeType shape,int horizontal,int vertical){
  int max=Math.max(horizontal,vertical);
  // Preserve unusual/overflow-prone native configurations rather than assume ideal geometry.
  if(original<=0||horizontal<=0||vertical<=0||max>4096||shape==null)return original;
  double bound;
  if(shape==SpawnShapeType.ELLIPSOID)bound=max;
  else if(shape==SpawnShapeType.CYLINDER)bound=Math.hypot(horizontal,vertical);
  else if(shape==SpawnShapeType.CUBOID)bound=Math.sqrt(2.0*horizontal*horizontal+(double)vertical*vertical);
  else return original;
  // One-block outward margin covers float rounding in the native ellipsoid predicate.
  return Math.min(original,(int)Math.ceil(bound)+1);
 }
}
