/**
 * Copyright 2005 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Originally based on Project64 code.
 *
 */


package kernel64gl.engine.rdp.combiners;

import static kernel64gl.engine.rdp.combiners.Combiners.*;

/** Simplifies RDP cycles and merges compatible combiner stages. */
final class CombinerStages {
    private CombinerStages() {}
    static void simplify(CombinerStage stage, CombineCycle cc) {
            // Load the first operand
            stage.op[0].op = LOAD;
            stage.op[0].param1 = cc.sa;
            stage.numOps = 1;
            
            // If we're just subtracting zero, skip it
            if (cc.sb != ZERO) {
                // Subtracting a number from itself is zero
                if (cc.sb == stage.op[0].param1)
                    stage.op[0].param1 = ZERO;
                else {
                    stage.op[1].op = SUB;
                    stage.op[1].param1 = cc.sb;
                    stage.numOps++;
                }
            }
            
            // If we either subtracted, or didn't load a zero
            if ((stage.numOps > 1) || (stage.op[0].param1 != ZERO)) {
                // Multiplying by zero is zero
                if (cc.m == ZERO) {
                    stage.numOps = 1;
                    stage.op[0].op = LOAD;
                    stage.op[0].param1 = ZERO;
                } else {
                    // Multiplying by one, so just do a load
                    if ((stage.numOps == 1) && (stage.op[0].param1 == ONE))
                        stage.op[0].param1 = cc.m;
                    else {
                        stage.op[stage.numOps].op = MUL;
                        stage.op[stage.numOps].param1 = cc.m;
                        stage.numOps++;
                    }
                }
            }
            
            // Don't bother adding zero
            if (cc.a != ZERO) {
                // If all we have so far is zero, then load this instead
                if ((stage.numOps == 1) && (stage.op[0].param1 == ZERO))
                    stage.op[0].param1 = cc.a;
                else {
                    stage.op[stage.numOps].op = ADD;
                    stage.op[stage.numOps].param1 = cc.a;
                    stage.numOps++;
                }
            }
            
            // Handle interpolation
            if ((stage.numOps == 4) && (stage.op[1].param1 == stage.op[3].param1)) {
                stage.numOps = 1;
                stage.op[0].op = INTER;
                stage.op[0].param2 = stage.op[1].param1;
                stage.op[0].param3 = stage.op[2].param1;
            }
            }
    static void merge(Combiner combiner) {
            // If all we have is a load in the first combiner.stage we can just replace
            // each occurance of COMBINED in the second combiner.stage with it
            if ((combiner.stage[0].numOps == 1) && (combiner.stage[0].op[0].op == LOAD)) {
                int combined = combiner.stage[0].op[0].param1;
                
                for (int i = 0; i < combiner.stage[1].numOps; i++) {
                    combiner.stage[0].op[i].op = combiner.stage[1].op[i].op;
                    combiner.stage[0].op[i].param1 = (combiner.stage[1].op[i].param1 == COMBINED) ? combined : combiner.stage[1].op[i].param1;
                    combiner.stage[0].op[i].param2 = (combiner.stage[1].op[i].param2 == COMBINED) ? combined : combiner.stage[1].op[i].param2;
                    combiner.stage[0].op[i].param3 = (combiner.stage[1].op[i].param3 == COMBINED) ? combined : combiner.stage[1].op[i].param3;
                }
                
                combiner.stage[0].numOps = combiner.stage[1].numOps;
                combiner.numStages = 1;
            }
            // We can't do any merging on an interpolation
            else if (combiner.stage[1].op[0].op != INTER) {
                int numCombined = 0;
                
                // See how many times the first combiner.stage is used in the second one
                for (int i = 0; i < combiner.stage[1].numOps; i++)
                    if (combiner.stage[1].op[i].param1 == COMBINED)
                        numCombined++;
                
                // If it's not used, just replace the first combiner.stage with the second
                if (numCombined == 0) {
                    for (int i = 0; i < combiner.stage[1].numOps; i++) {
                        combiner.stage[0].op[i].op = combiner.stage[1].op[i].op;
                        combiner.stage[0].op[i].param1 = combiner.stage[1].op[i].param1;
                        combiner.stage[0].op[i].param2 = combiner.stage[1].op[i].param2;
                        combiner.stage[0].op[i].param3 = combiner.stage[1].op[i].param3;
                    }
                    combiner.stage[0].numOps = combiner.stage[1].numOps;
                    
                    combiner.numStages = 1;
                }
                // If it's only used once
                else if (numCombined == 1) {
                    // It's only used in the load, so tack on the ops from combiner.stage 2 on combiner.stage 1
                    if (combiner.stage[1].op[0].param1 == COMBINED) {
                        for (int i = 1; i < combiner.stage[1].numOps; i++) {
                            combiner.stage[0].op[combiner.stage[0].numOps].op = combiner.stage[1].op[i].op;
                            combiner.stage[0].op[combiner.stage[0].numOps].param1 = combiner.stage[1].op[i].param1;
                            combiner.stage[0].numOps++;
                        }
                        
                        combiner.numStages = 1;
                    }
                    // Otherwise, if it's used in the second op, and that op isn't SUB
                    // we can switch the parameters so it works out to tack the ops onto combiner.stage 1
                    else if ((combiner.stage[1].op[1].param1 == COMBINED) && (combiner.stage[1].op[1].op != SUB)) {
                        combiner.stage[0].op[combiner.stage[0].numOps].op = combiner.stage[1].op[1].op;
                        combiner.stage[0].op[combiner.stage[0].numOps].param1 = combiner.stage[1].op[0].param1;
                        combiner.stage[0].numOps++;
                        
                        // If there's another op, tack it onto combiner.stage 1 too
                        if (combiner.stage[1].numOps > 2) {
                            combiner.stage[0].op[combiner.stage[0].numOps].op = combiner.stage[1].op[2].op;
                            combiner.stage[0].op[combiner.stage[0].numOps].param1 = combiner.stage[1].op[2].param1;
                            combiner.stage[0].numOps++;
                        }
                        
                        combiner.numStages = 1;
                    }
                }
            }
            }
}
