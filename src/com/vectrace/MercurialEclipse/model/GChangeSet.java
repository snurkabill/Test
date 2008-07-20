/**
 * 
 */
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vectrace.MercurialEclipse.model.GChangeSet.Edge.EdgeType;

/**
 * Don't look at this code - your eyes will bleed.
 * 
 * Seriously. Turn back now.
 */
public class GChangeSet {

	private EdgeList middle = new EdgeList(true);
	private EdgeList after = new EdgeList(false);
	private int index;
	private final RowCount rowCount;

	public GChangeSet(RowCount rowCount, int index, String middleS,
			String afterS) {
		this.rowCount = rowCount;
		this.index = index;
		middle.parse(middleS);
		after.parse(afterS);
	}

	public GChangeSet clean(GChangeSet last) {
		middle.clean(last);
		return this;
	}

	public int getIndex() {
		return index;
	}

	public EdgeList getBefore() {
		return getMiddle();
	}

	public EdgeList getMiddle() {
		return middle;
	}

	public EdgeList getAfter() {
		return after;
	}

	public class EdgeList {

		private List<Edge> edges = new ArrayList<Edge>();
		private Set<Integer> above = new HashSet<Integer>();
		private boolean straight;
		private int[] jumps;

		public EdgeList(boolean straight) {
			this.straight = straight;
		}

		public void parse(String string) {
			int length = string.length();
			int count = 0;
			for (int i = 0; i < length; i++) {
				count += addEdge(string, i, count);
			}
			rowCount.endRow();
			if (string.contains("+")) {
				rowCount.jump = string.indexOf('o');
				jumps = new int[] { string.indexOf('+') / 2, rowCount.jump / 2, };
			}
		}

		private int addEdge(String string, int i, int count) {
			char c = string.charAt(i);
			if (c == ' ') {
				return rowCount.space(i, count);
			}
			Edge edge = new Edge(c, count);
			return rowCount.update(this, edge);
		}

		public int[] getJump() {
			return jumps;
		}

		public void add(Edge edge) {
			if (straight) {
				edge.straighten();
			}
			above.add(edge.bottom);
			edges.add(edge);
		}

		public List<Edge> getEdges() {
			return edges;
		}

		public void clean(GChangeSet last) {
			for (Edge e : edges) {
				e.setFinish(e.isDot()
						&& (last == null || !last.after.above.contains(e.top)));
			}
		}
	}

	public static class RowCount {
		public int jump;
		public List<Integer> cols = new ArrayList<Integer>();
		private int unique = 0;
		private Edge lastEdge;
		private int dec = -1;

		public RowCount() {
			cols.add(0);
		}

		public int space(int i, int count) {
			lastEdge = null;
			if (jump == i) {
				dec = count;
				return 1;
			}
			return 0;
		}

		public int update(EdgeList edges, Edge edge) {
			Integer col;
			boolean lastLine = lastEdge != null
					&& lastEdge.type == EdgeType.line;
			int count = 1;
			if (edge.type == EdgeType.backslash && lastLine) {
				unique++;
				cols.add(edge.col, col = unique);
			} else if (edge.type == EdgeType.slash && lastLine) {
				dec = edge.col;
				col = cols.get(edge.col);
			} else if (edge.type == EdgeType.line && lastEdge != null
					&& lastEdge.type == EdgeType.backslash) {
				count = 0;
				edge.dec();
				cols.remove(edge.col);
				col = cols.get(edge.col);
			} else if (edge.type == EdgeType.line && lastEdge != null
                    && lastEdge.type == EdgeType.slash) {
                count = 0;
                edge.dec();
                col = cols.get(edge.col);
                dec = -1;
            } else if (edge.type == EdgeType.dash
					&& (lastEdge == null || lastEdge.type != EdgeType.dash)) {
				lastEdge = edge;
				return 0;
			} else if (edge.col >= cols.size()) {
				unique++;
				cols.add(col = unique);
			} else {
				col = cols.get(edge.col);
			}
			edge.lane = col;
			if (edge.type == EdgeType.dash) {
				lastEdge = null;
			} else {
				lastEdge = edge;
			}
			edges.add(edge);
			return count;
		}

		public void endRow() {
			lastEdge = null;
			if (dec > -1) {
				cols.remove(dec);
				dec = -1;
			}
			jump = -1;
		}
	}

	public static class Edge {
		public static enum EdgeType {
			line, dot, working, plus, dash, slash, backslash
		}

		private int top, bottom, col;
		private int lane;
		private boolean finish;
		private EdgeType type;

		public Edge(char c, int i) {
			col = top = bottom = i;
			type = EdgeType.line;
			switch (c) {
			case '/':
				type = EdgeType.slash;
				bottom--;
				break;
			case '\\':
				type = EdgeType.backslash;
				top--;
				break;
			case 'o':
				type = EdgeType.dot;
				break;
			case '@':
				type = EdgeType.working;
				break;
			case '+':
				type = EdgeType.plus;
				break;
			case '-':
				type = EdgeType.dash;
				break;
			}
		}

		public void setFinish(boolean finish) {
			this.finish = finish;
		}

		public boolean isFinish() {
			return finish;
		}

		public void straighten() {
			top = bottom = Math.max(top, bottom);
		}

		public void dec() {
			top = bottom = col = col - 1;
		}

		public int getTop() {
			return top;
		}

		public int getBottom() {
			return bottom;
		}

		public int getLane() {
			return lane;
		}

		public boolean isDot() {
			return type == EdgeType.dot || type == EdgeType.working
					|| type == EdgeType.plus;
		}

		public boolean isPlus() {
			return type == EdgeType.plus;
		}
	}
}