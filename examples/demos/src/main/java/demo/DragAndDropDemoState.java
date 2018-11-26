/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package demo;

import java.util.*;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.input.event.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.*;

import com.jme3.scene.shape.Line;
import com.simsilica.lemur.*;
import com.simsilica.lemur.dnd.*;
import com.simsilica.lemur.core.GuiMaterial;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.style.BaseStyles;
import javafx.util.Pair;

/**
 *  Demo and test of the drag-and-drop support in Lemur.
 *
 *  @author    Paul Speed
 */
public class DragAndDropDemoState extends SimpleApplication {

    private ColorRGBA containerColor = new ColorRGBA(1, 1, 0, 0.5f);
    private ColorRGBA containerHighlight = new ColorRGBA(0, 1, 0, 0.5f);

    private ContainerNode container1;
    private ContainerNode container2;

    private AssetManager assetManager;

    private static final int GRID_SIZE = 5;
    private static final float LOCAL_SCALE = 13.5f;

    public DragAndDropDemoState() {
    }

    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
        setDisplayFps(false);
        setDisplayStatView(false);

        GuiGlobals.initialize(this);

        GuiGlobals globals = GuiGlobals.getInstance();
        BaseStyles.loadGlassStyle();
        globals.getStyles().setDefaultStyle("glass");
        initialize(this);
    }

    protected Node getRoot() {
        return guiNode;
    }

    protected void initialize( Application app ) {
        this.assetManager = app.getAssetManager();

        container1 = new ContainerNode("container2", containerColor);
        container1.setSize(GRID_SIZE, GRID_SIZE, 0);
        container1.setLocalTranslation(200f, 100f, 0.5f);
        container1.setLocalScale(LOCAL_SCALE);
        MouseEventControl.addListenersToSpatial(container1,
                new HighlightListener(container1.material,
                        containerHighlight,
                        containerColor));
        container1.addControl(new GridControl(GRID_SIZE));
        container1.addControl(new DragAndDropControl(new GridContainerListener(container1)));
        getRoot().attachChild(container1);

        // Add some random items to our MVC grid 'model' control
        container1.getControl(GridControl.class).setCell(0, 0, createItem());
        container1.getControl(GridControl.class).setCell(2, 1, createItem());

        // Setup a grid based container
        container2 = new ContainerNode("container2", containerColor);
        container2.setSize(GRID_SIZE, GRID_SIZE, 0);
        container2.setLocalTranslation(400f, 100f, 0.5f);
        container2.setLocalScale(LOCAL_SCALE);
        MouseEventControl.addListenersToSpatial(container2,
                new HighlightListener(container2.material,
                        containerHighlight,
                        containerColor));
        container2.addControl(new GridControl(GRID_SIZE));
        container2.addControl(new DragAndDropControl(new GridContainerListener(container2)));
        getRoot().attachChild(container2);

        // Add some random items to our MVC grid 'model' control
        container2.getControl(GridControl.class).setCell(0, 0, createItem());
        container2.getControl(GridControl.class).setCell(2, 1, createItem());
    }

    private Spatial createItem() {
        Sphere sphere = new Sphere(12, 24, 0.9f);
        Geometry geom = new Geometry("item", sphere);

        // Create a random color
        float r = (float)(Math.random() * 0.4 + 0.2);
        float g = (float)(Math.random() * 0.6 + 0.2);
        float b = (float)(Math.random() * 0.6 + 0.2);
        //ColorRGBA color = new ColorRGBA(r, g, b, 1);

        geom.setMaterial(new Material( assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        return geom;
    }

    /**
     *  Just to encapsulate the visuals needed to have both a wireframe
     *  view but an actual box for picking.
     */
    private class ContainerNode extends Node {

        private GuiMaterial material;
        private WireBox wire;
        private Geometry wireGeom;
        private Box box;
        private Geometry boxGeom;

        public ContainerNode( String name, ColorRGBA color ) {
            super(name);
            material = GuiGlobals.getInstance().createMaterial(containerColor, false);

            List<Pair<Vector3f, Vector3f>> lines = new ArrayList<>();
            // horizontal lines
            for (float column = 2 - GRID_SIZE; column < GRID_SIZE; column += 2) {
                lines.add(new Pair<>(new Vector3f(-GRID_SIZE, column, 0f), new Vector3f(GRID_SIZE, column, 0f)));
            }
            for (float row = 2 - GRID_SIZE; row < GRID_SIZE; row += 2) {
                lines.add(new Pair<>(new Vector3f(row, -GRID_SIZE,  0f), new Vector3f(row, GRID_SIZE, 0f)));
            }
            drawLines(lines);

            wire = new WireBox(1, 1, 0);
            wireGeom = new Geometry(name + ".wire", wire);
            wireGeom.setMaterial(material.getMaterial());
            attachChild(wireGeom);

            box = new Box(1, 1, 0);
            boxGeom = new Geometry(name + ".box", box);
            boxGeom.setMaterial(material.getMaterial()); // might as well reuse it
            boxGeom.setCullHint(CullHint.Always); // invisible
            attachChild(boxGeom);
        }

        private void drawLines(List<Pair<Vector3f, Vector3f>> lines ) {
            for (Pair<Vector3f, Vector3f> line : lines) {
                Line l = new Line(line.getKey(), line.getValue());
                Geometry lineGeometry = new Geometry("line", l);
                Material lineMaterial = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
                lineGeometry.setMaterial(lineMaterial);
                attachChild(lineGeometry);
            }
        }

        private void setSize( float x, float y, float z ) {
            wire.updatePositions(x, y, z);
            box.updateGeometry(Vector3f.ZERO, x, y, z);
            box.clearCollisionData();

            wireGeom.updateModelBound();
            boxGeom.updateModelBound();
        }
    }

    private class GridControl extends AbstractControl {

        private ContainerNode node;
        private int gridSize;
        private Spatial[][] grid;

        public GridControl( int gridSize ) {
            this.gridSize = gridSize;
            this.grid = new Spatial[gridSize][gridSize];
        }

        @Override
        public void setSpatial( Spatial s ) {
            super.setSpatial(s);
            this.node = (ContainerNode)s;
            updateLayout();
        }

        public Spatial getCell( int x, int y ) {
            return grid[x][y];
        }

        public void setCell( int x, int y, Spatial child ) {
            if( grid[x][y] != null ) {
                grid[x][y].removeFromParent();
            }
            grid[x][y] = child;
            if( child != null ) {
                node.attachChild(child);
            }
            updateLayout();
        }

        public Spatial removeCell( int x, int y ) {
            Spatial result = grid[x][y];
            grid[x][y] = null;
            if( result != null ) {
                updateLayout();
            }
            return result;
        }

        public void addChild( Spatial child ) {
            // Find the first valid cell
            for( int x = 0; x < gridSize; x++ ) {
                for( int y = 0; y < gridSize; y++ ) {
                    // just in case the child is already in the grid
                    if( grid[x][y] == child ) {
                        return;
                    }
                    if( grid[x][y] == null ) {
                        setCell(x, y, child);
                        return;
                    }
                }
            }
        }

        public void removeChild( Spatial child ) {
            for( int x = 0; x < gridSize; x++ ) {
                for( int y = 0; y < gridSize; y++ ) {
                    if( child == grid[x][y] ) {
                        if( child.getParent() == node ) {
                            child.removeFromParent();
                        }
                        grid[x][y] = null;
                    }
                }
            }
            updateLayout();
        }

        protected void updateLayout() {
            node.setSize(gridSize, gridSize, 0);
            for( int x = 0; x < gridSize; x++ ) {
                for( int y = 0; y < gridSize; y++ ) {
                    Spatial child = grid[x][y];
                    if( child != null ) {
                        child.setLocalTranslation(-(gridSize - 1) + x * 2, (gridSize - 1) - y * 2, 0);
                    }
                }
            }
        }

        @Override
        protected void controlUpdate( float tpf ) {
        }

        @Override
        protected void controlRender( RenderManager rm, ViewPort vp ) {
        }
    }

    /**
     *  Listens for enter/exit events and changes the color of
     *  the geometry accordingly.
     */
    private class HighlightListener extends DefaultMouseListener {
        private GuiMaterial material;
        private ColorRGBA enterColor;
        private ColorRGBA exitColor;

        public HighlightListener( GuiMaterial material, ColorRGBA enterColor, ColorRGBA exitColor ) {
            this.material = material;
            this.enterColor = enterColor;
            this.exitColor = exitColor;
        }

        public void mouseEntered( MouseMotionEvent event, Spatial target, Spatial capture ) {
            material.setColor(enterColor);
        }

        public void mouseExited( MouseMotionEvent event, Spatial target, Spatial capture ) {
            material.setColor(exitColor);
        }
    }

    private class GridContainerListener implements DragAndDropListener {

        private Spatial container;

        public GridContainerListener( Spatial container ) {
            this.container = container;
        }

        /**
         *  Returns the container 'model' (in the MVC sense) for this
         *  container listener.
         */
        public GridControl getModel() {
            return container.getControl(GridControl.class);
        }

        private Vector2f getCellLocation( Vector3f world ) {
            Vector3f local = container.worldToLocal(world, null);

            // Calculate the cell location
            float x = (GRID_SIZE + local.x) / 2;
            float y = (GRID_SIZE - local.y) / 2;

            // This will look a little off to the user towards the right edge because
            // clicking on the surface of the box in the center cell will actually project
            // into the sphere in the last column.  But it works for a demo.  We could
            // also have made a ray and done collideWith() on the childre but I wanted
            // to show model-cell interaction instead of picking.
            int xCell = (int)x;
            int yCell = (int)y;

            return new Vector2f(xCell, yCell);
        }

        public Draggable onDragDetected( DragEvent event ) {

            System.out.println("Grid.onDragDetected(" + event + ")");

            // Find the child we collided with
            GridControl control = getModel();

            // See where we hit
            Vector2f hit = getCellLocation(event.getCollision().getContactPoint());

            // Remove the item from the grid if it exists.
            Spatial item = control.removeCell((int)hit.x, (int)hit.y);
            if( item != null ) {
                // Save the item in the session so the other containers (and ourselves)
                // know what we are dragging.
                event.getSession().set(DragSession.ITEM, item);

                // We'll keep track of the grid cell in case the drag is
                // canceled and we have to put it back.
                event.getSession().set("gridLocation", hit);

                // Clone the dragged item to use in our draggable and stick the
                // clone in the root at the same world location.
                Spatial drag = item.clone();
                drag.setLocalTranslation(item.getWorldTranslation());
                drag.setLocalRotation(item.getWorldRotation());
                drag.setLocalScale(LOCAL_SCALE);
                getRoot().attachChild(drag);

                // Now that we've got the world location of the item we can remove
                // it from the parent spatial since it is not really a child anymore.
                // We only left it so we could easily get its world location/rotation.
                item.removeFromParent();

                return new ColoredDraggable(event.getViewPort(), drag, event.getLocation());
            }
            return null;
        }

        public void onDragEnter( DragEvent event ) {
            System.out.println("+++++++ Grid.onDragEnter(" + event + ")");
        }

        public void onDragExit( DragEvent event ) {
            System.out.println("------- Grid.onDragExit(" + event + ")");
        }

        public void onDragOver( DragEvent event ) {
            System.out.println("Grid.onDragOver(" + event + ")");

            Vector2f hit = getCellLocation(event.getCollision().getContactPoint());
            Spatial item = getModel().getCell((int)hit.x, (int)hit.y);
            if( item == null ) {
                // An empty cell is a valid target
                event.getSession().setDragStatus(DragStatus.ValidTarget);
            } else {
                // A filled slot is not
                event.getSession().setDragStatus(DragStatus.InvalidTarget);
            }
        }

        // Target specific
        public void onDrop( DragEvent event ) {
            System.out.println("Grid.onDrop(" + event + ")");

            Spatial draggedItem = event.getSession().get(DragSession.ITEM, null);

            Vector2f hit = getCellLocation(event.getCollision().getContactPoint());

            // One last check to see if the drop location is available
            Spatial item = getModel().getCell((int)hit.x, (int)hit.y);
            if( item == null ) {
                // Then we can stick the new child right in
                getModel().setCell((int)hit.x, (int)hit.y, draggedItem);
            } else {
                // It wasn't really a valid drop
                event.getSession().setDragStatus(DragStatus.InvalidTarget);
            }
        }

        // Source specific
        public void onDragDone( DragEvent event ) {
            System.out.println("Grid.onDragDone(" + event + ")");

            DragSession session = event.getSession();

            // Check to see if drop target was null as this indicates
            // that the drag operation didn't finish and we need to
            // put the item back.
            if( session.getDropTarget() == null ) {

                // Grab the payload we stored during drag start
                Spatial draggedItem = session.get(DragSession.ITEM, null);

                // Grab the original slot of the item.  We tucked this away
                // during drag start just for this case.
                Vector2f slot = session.get("gridLocation", null);
                if( slot != null ) {
                    getModel().setCell((int)slot.x, (int)slot.y, draggedItem);
                } else {
                    System.out.println("Error, missing gridLocation for dragged item");
                    // This should not ever happen but if it does we'll at least
                    // try to deal with it
                    getModel().addChild(draggedItem);
                }
            }
        }
    }

    private class ColoredDraggable extends DefaultDraggable {

        private Material originalMaterial;
        private Geometry geom;

        public ColoredDraggable( ViewPort view, Spatial spatial, Vector2f start ) {
            super(view, spatial, start);
            this.geom = (Geometry)spatial;
            this.originalMaterial = geom.getMaterial();
        }

        @Override
        public void updateDragStatus( DragStatus status ) {
            switch( status ) {
                case ValidTarget:
                    geom.setMaterial(originalMaterial);
                    break;
                default:
                    break;
            }
        }
    }
}
