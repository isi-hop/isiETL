/*
 * Copyright (C) 2025 tondeur-h
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
 */
package org.isihop.fr.isietl;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author tondeur-h
 */
@Data
@NoArgsConstructor
class Integrator {
    public String connector;
    public String exttype;
    public String filespath;
    public String checkfiles;
    //description des champs input
    public Map<String,Fields> fieldsIn;
  
    //output data
    public String destination;
    public String dbdriver;
    public String dblogin;
    public String dbpassword;
    
    //upsert constructor
    public String sqlinsert;
    public String sqlupdate;
    //la clé de contrôle se le hascode calcul dans un champs varchar
}
