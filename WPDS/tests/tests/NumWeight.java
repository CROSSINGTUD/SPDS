package tests;

import wpds.impl.Weight;
import wpds.interfaces.Location;

public class NumWeight<N extends Location> extends Weight<N> {

	private int i;

	public NumWeight(int i) {
		this.i = i;
	}

	private NumWeight() {
	}

	@Override
	public Weight<N> extendWith(Weight<N> other) {
		if (this.equals(one()))
			return other;
		if (other.equals(one()))
			return this;

		NumWeight<N> o = (NumWeight<N>) other;
		return new NumWeight<N>(o.i + i);
	}

	@Override
	public Weight<N> combineWith(Weight<N> other) {
		if (other.equals(zero()))
			return this;
		if (this.equals(zero()))
			return other;
		NumWeight<N> o = (NumWeight<N>) other;
		if (o.i == i)
			return o;
		return zero();
	}

	private static NumWeight one;

	public static <N extends Location> NumWeight<N> one() {
		if (one == null)
			one = new NumWeight<N>() {
				@Override
				public String toString() {
					return "<ONE>";
				}
				@Override
				public boolean equals(Object obj) {
					return obj == this;
				}
			};
		return one;
	}

	private static NumWeight zero;

	public static <N extends Location> NumWeight<N> zero() {
		if (zero == null)
			zero = new NumWeight<N>() {
				@Override
				public String toString() {
					return "<ZERO>";
				}

				@Override
				public boolean equals(Object obj) {
					return obj == this;
				}
			};
		return zero;
	}

	@Override
	public String toString() {
		return Integer.toString(i);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + i;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NumWeight other = (NumWeight) obj;
		if (i != other.i)
			return false;
		return true;
	}

}
