package boomerang.scene.jimple;

import boomerang.scene.WrappedClass;
import java.nio.file.Path;
import java.util.Optional;

/** @author Manuel Benz at 2019-08-12 */
public interface ISourceLocator {
  /**
   * Computes the path to the given clazz in the {@link Project} this {@link ISourceLocator} belongs
   * to.
   *
   * @param clazz
   * @return Relative or absolute to the clazz's source.
   */
  Optional<Path> locateClass(WrappedClass clazz);

  /**
   * Computes the relative path to the given clazz in the {@link Project} this {@link
   * ISourceLocator} belongs to. This path is relative to the {@link ISourceLocator}'s base path.
   *
   * @param clazz
   * @return Relative to the clazz's source.
   */
  default Optional<Path> locateClassRelative(WrappedClass clazz) {
    // make sure to use the absolute paths here
    return locateClass(clazz)
        .map(
            p ->
                p.toAbsolutePath()
                    // for some reason relativize+normalize does not do the thing
                    .subpath(getBasePath().toAbsolutePath().getNameCount(), p.getNameCount()));
  }

  /**
   * Returns the base path to which all other paths are relativ located. This should be the root
   * path of the {@link Project} this {@link ISourceLocator} belongs to.
   *
   * @return
   */
  Path getBasePath();
}
